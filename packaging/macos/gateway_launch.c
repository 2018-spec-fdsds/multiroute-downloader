/* Fixed graphical-authorization target. Its monitor drops privileges immediately
 * and continuously drains the capture component while retaining at most 64 KiB.
 * No routing, BPF permission changes, user-file redirection or arbitrary exec. */
#define _DARWIN_C_SOURCE 1
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <limits.h>
#include <libproc.h>
#include <pwd.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#ifndef MULTIROUTE_VERSION
#error MULTIROUTE_VERSION must match the saved Java application version
#endif
#define HELPER_DIRECTORY "/Library/PrivilegedHelperTools/MultiRouteResume/" MULTIROUTE_VERSION
#define HELPER_PATH HELPER_DIRECTORY "/gateway-net"
#define OUTPUT_LIMIT 65536u

static int number(const char *text, unsigned long *value) {
    char *end;
    if (!text || !*text || text[0] < '1' || text[0] > '9') return 0;
    errno = 0;
    *value = strtoul(text, &end, 10);
    return errno == 0 && *end == 0 && *value <= INT_MAX;
}

static int owner_identity(uid_t uid, gid_t gid, pid_t parent) {
    struct passwd *account = getpwuid(uid);
    struct proc_bsdinfo info;
    if (!uid || !gid || parent < 1 || !account || account->pw_gid != gid) return 0;
    if (proc_pidinfo(parent, PROC_PIDTBSDINFO, 0, &info, sizeof(info)) != sizeof(info)) return 0;
    return info.pbi_uid == uid && info.pbi_ruid == uid && info.pbi_gid == gid && info.pbi_rgid == gid;
}

static int permanently_drop(uid_t uid, gid_t gid) {
    if (geteuid() != 0) return getuid() == uid && geteuid() == uid && getgid() == gid && getegid() == gid;
    if (setgroups(0, NULL) || setgid(gid) || setuid(uid)) return 0;
    return getuid() == uid && geteuid() == uid && getgid() == gid && getegid() == gid && setuid(0) < 0;
}

static int ipv4(const char *text, uint32_t *value) {
    uint32_t result = 0;
    int part;
    for (part = 0; part < 4; part++) {
        unsigned octet = 0, digits = 0;
        const char *start = text;
        while (*text >= '0' && *text <= '9') {
            octet = octet * 10 + (unsigned)(*text++ - '0');
            if (++digits > 3 || octet > 255) return 0;
        }
        if (!digits || (digits > 1 && *start == '0')) return 0;
        result = (result << 8) | octet;
        if (part == 3) { if (*text != 0) return 0; }
        else if (*text++ != '.') return 0;
    }
    *value = result;
    return 1;
}

static int gateway_list(const char *text, uint32_t virtual_ip) {
    uint32_t seen[32], value;
    size_t count = 0;
    while (*text) {
        char address[16];
        size_t size = strcspn(text, ","), i;
        if (!size || size >= sizeof(address) || count == 32) return 0;
        memcpy(address, text, size); address[size] = 0;
        if (!ipv4(address, &value) || value == virtual_ip) return 0;
        for (i = 0; i < count; i++) if (seen[i] == value) return 0;
        seen[count++] = value;
        text += size;
        if (*text == ',') { text++; if (!*text) return 0; }
    }
    return count > 0;
}

static int clean_text(const char *text) {
    const unsigned char *cursor = (const unsigned char *)text;
    if (!text || !*text || strlen(text) > 4095) return 0;
    while (*cursor) { if (*cursor < 32 || *cursor == 127) return 0; cursor++; }
    return 1;
}

static int state_paths(const char *ready, const char *stop) {
    const char *name;
    size_t length, i;
    if (!clean_text(ready) || !clean_text(stop) || ready[0] != '/' || stop[0] != '/') return 0;
    length = strlen(ready);
    if (strlen(stop) != length + 5 || strncmp(ready, stop, length) || strcmp(stop + length, ".stop")) return 0;
    name = strrchr(ready, '/');
    if (!name || strncmp(++name, "ready-", 6) || strlen(name) != 47 || strcmp(name + 42, ".json")) return 0;
    for (i = 6; i < 42; i++) {
        if (i == 14 || i == 19 || i == 24 || i == 29) { if (name[i] != '-') return 0; }
        else if (!((name[i] >= '0' && name[i] <= '9') || (name[i] >= 'a' && name[i] <= 'f'))) return 0;
    }
    return 1;
}

static int protected_path(const char *path, int directory) {
    struct stat status;
    if (lstat(path, &status) || status.st_uid != 0 || (status.st_mode & 0022)) return 0;
    return directory ? S_ISDIR(status.st_mode) : S_ISREG(status.st_mode) && status.st_nlink == 1;
}

static int protected_helper(void) {
    return protected_path("/Library", 1) && protected_path("/Library/PrivilegedHelperTools", 1) &&
        protected_path("/Library/PrivilegedHelperTools/MultiRouteResume", 1) &&
        protected_path(HELPER_DIRECTORY, 1) && protected_path(HELPER_PATH, 0) && access(HELPER_PATH, X_OK) == 0;
}

static int close_inherited(int preserve) {
    struct rlimit limit;
    rlim_t descriptor;
    if (getrlimit(RLIMIT_NOFILE, &limit) || limit.rlim_cur == RLIM_INFINITY || limit.rlim_cur > INT_MAX) return 0;
    for (descriptor = 3; descriptor < limit.rlim_cur; descriptor++)
        if ((int)descriptor != preserve) close((int)descriptor);
    return 1;
}

static int write_all(int destination, const unsigned char *data, size_t size) {
    while (size) {
        ssize_t written = write(destination, data, size);
        if (written < 0 && errno == EINTR) continue;
        if (written <= 0) return 0;
        data += written; size -= (size_t)written;
    }
    return 1;
}

static int bounded_drain(int source, int destination, uint64_t *received, size_t *retained) {
    unsigned char buffer[8192];
    int writable = 1;
    *received = 0; *retained = 0;
    for (;;) {
        ssize_t count = read(source, buffer, sizeof(buffer));
        size_t keep;
        if (count < 0 && errno == EINTR) continue;
        if (count < 0) return 0;
        if (count == 0) return 1;
        *received += (uint64_t)count;
        keep = (size_t)count < OUTPUT_LIMIT - *retained ? (size_t)count : OUTPUT_LIMIT - *retained;
        if (writable && keep) {
            if (write_all(destination, buffer, keep)) *retained += keep;
            else writable = 0; /* Still drain the entire child stream after EPIPE. */
        }
    }
}

static int child_status(pid_t child) {
    int status;
    while (waitpid(child, &status, 0) < 0) { if (errno != EINTR) return 125; }
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
    return 125;
}

static int drain_fixture(int destination, int expected_retained, int expected_exit) {
    int pipes[2];
    pid_t child;
    uint64_t received;
    size_t retained;
    if (pipe(pipes)) return 0;
    child = fork();
    if (child < 0) { close(pipes[0]); close(pipes[1]); return 0; }
    if (!child) {
        unsigned char buffer[8192]; size_t count;
        close(pipes[0]); memset(buffer, 'A', sizeof(buffer));
        for (count = 0; count < 128; count++) if (!write_all(pipes[1], buffer, sizeof(buffer))) _exit(90);
        close(pipes[1]); _exit(expected_exit);
    }
    close(pipes[1]);
    if (!bounded_drain(pipes[0], destination, &received, &retained)) { close(pipes[0]); child_status(child); return 0; }
    close(pipes[0]);
    return child_status(child) == expected_exit && received == 1048576 && retained == (size_t)expected_retained;
}

static int self_test(void) {
    int sink = open("/dev/null", O_WRONLY), broken[2];
    if (sink < 0 || pipe(broken)) return 91;
    signal(SIGPIPE, SIG_IGN);
    if (!drain_fixture(sink, OUTPUT_LIMIT, 23)) return 92;
    close(sink); close(broken[0]);
    if (!drain_fixture(broken[1], 0, 17)) return 93;
    close(broken[1]);
    puts("GATEWAY_LAUNCH_SELF_TEST_OK 1MiB fully drained, 64KiB retained, exit codes preserved, closed consumer tolerated; no network handles");
    return 0;
}

int main(int argc, char **argv) {
    const char *values[7] = {0};
    const char *options[] = {"--virtual-ip", "--gateways", "--ready-file", "--parent-pid", "--drop-uid", "--drop-gid", "--stop-file"};
    unsigned long owner, group, parent;
    uint32_t virtual_ip;
    int i, j, pipes[2], drained;
    pid_t child;
    uint64_t received;
    size_t retained;
    char *environment[] = {"PATH=/usr/bin:/bin:/usr/sbin:/sbin", "LANG=en_US.UTF-8", NULL};
    if (argc == 2 && !strcmp(argv[1], "--self-test")) return self_test();
    if (argc == 5 && !strcmp(argv[1], "--self-test-drop")) {
        if (!number(argv[2], &owner) || !number(argv[3], &group) || !number(argv[4], &parent) ||
            !owner_identity((uid_t)owner, (gid_t)group, (pid_t)parent) || !permanently_drop((uid_t)owner, (gid_t)group)) return 94;
        printf("GATEWAY_LAUNCH_DROP_OK uid=%lu gid=%lu cannot regain root\n", (unsigned long)geteuid(), (unsigned long)getegid());
        return 0;
    }
    if (argc != 16 || strcmp(argv[1], HELPER_PATH)) { fputs("Only the installed gateway component is permitted\n", stderr); return 2; }
    for (i = 2; i < argc; i += 2) {
        for (j = 0; j < 7 && strcmp(argv[i], options[j]); j++) { }
        if (j == 7 || values[j] || !clean_text(argv[i + 1])) return 2;
        values[j] = argv[i + 1];
    }
    if (!ipv4(values[0], &virtual_ip) || !gateway_list(values[1], virtual_ip) || !state_paths(values[2], values[6]) ||
        !number(values[3], &parent) || !number(values[4], &owner) || !number(values[5], &group) ||
        !owner_identity((uid_t)owner, (gid_t)group, (pid_t)parent)) { fputs("Invalid gateway owner, parent or arguments\n", stderr); return 3; }
    if (!protected_helper()) { fputs("The gateway component must be protected and root-owned\n", stderr); return 3; }
    if (geteuid() != 0 && (getuid() != (uid_t)owner || getgid() != (gid_t)group)) return 3;
    if (pipe(pipes)) return 4;
    child = fork();
    if (child < 0) { close(pipes[0]); close(pipes[1]); return 4; }
    if (!child) {
        close(pipes[0]);
        if (dup2(pipes[1], STDOUT_FILENO) < 0 || dup2(pipes[1], STDERR_FILENO) < 0 || !close_inherited(-1)) _exit(125);
        execve(HELPER_PATH, &argv[1], environment);
        fputs("Cannot start the installed gateway component\n", stderr); _exit(127);
    }
    close(pipes[1]);
    if (!close_inherited(pipes[0]) || !permanently_drop((uid_t)owner, (gid_t)group)) {
        kill(child, SIGTERM); close(pipes[0]); child_status(child); return 5;
    }
    signal(SIGPIPE, SIG_IGN);
    drained = bounded_drain(pipes[0], STDOUT_FILENO, &received, &retained);
    close(pipes[0]);
    if (!drained) { kill(child, SIGTERM); child_status(child); return 6; }
    return child_status(child);
}
