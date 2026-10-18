export function validateUrl(text) {
  if (typeof text !== 'string' || text.length > 16384 || /[\u0000-\u001f\u007f]/u.test(text)) throw new Error(chrome.i18n.getMessage('invalidUrl'));
  const value = text.trim(), url = new URL(value);
  if (!['http:', 'https:', 'ftp:', 'ftps:', 'sftp:', 'webdavs:', 'magnet:'].includes(url.protocol) || url.username || url.password || url.hash || (url.protocol !== 'magnet:' && !url.hostname)) throw new Error(chrome.i18n.getMessage('invalidUrl'));
  return value;
}
export function portValue(value) {
  const port = Number(value);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error(chrome.i18n.getMessage('invalidPort'));
  return port;
}
export async function sendUrl(text, port) {
  const url = validateUrl(text);
  if (port === undefined) port = (await chrome.storage.local.get({port: 8765})).port;
  port = portValue(port);
  const response = await fetch(`http://127.0.0.1:${port}/api/v1/browser/download`, {
    method: 'POST', headers: {'Content-Type': 'application/json; charset=UTF-8'},
    body: JSON.stringify({url}), credentials: 'omit', redirect: 'error', signal: AbortSignal.timeout(10000)
  });
  if (response.status !== 202) throw new Error(chrome.i18n.getMessage(response.status === 403 ? 'enableIntegration' : 'openApplication'));
  const result = await response.json();
  if (result.accepted !== true) throw new Error(chrome.i18n.getMessage('openApplication'));
  return result;
}
