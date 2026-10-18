import {sendUrl, portValue} from './client.js';
const byId = id => document.getElementById(id);
for (const node of document.querySelectorAll('[data-i18n]')) node.textContent = chrome.i18n.getMessage(node.dataset.i18n);
document.documentElement.lang = chrome.i18n.getUILanguage();
document.documentElement.dir = chrome.i18n.getMessage('@@bidi_dir') || 'ltr';
try {
  const saved = await chrome.storage.local.get({port: 8765, lastError: ''});
  byId('port').value = saved.port;
  byId('status').textContent = saved.lastError;
  const [tab] = await chrome.tabs.query({active: true, currentWindow: true});
  if (tab?.url && /^(https?|ftps?|sftp|webdavs|magnet):/iu.test(tab.url)) byId('url').value = tab.url;
} catch { /* The URL can always be pasted manually without tab access. */ }
byId('send').addEventListener('click', async () => {
  const button = byId('send'); button.disabled = true; byId('status').textContent = chrome.i18n.getMessage('sending');
  try {
    const port = portValue(byId('port').value);
    await chrome.storage.local.set({port});
    await sendUrl(byId('url').value, port);
    byId('status').textContent = chrome.i18n.getMessage('sent');
    await chrome.storage.local.remove('lastError'); await chrome.action.setBadgeText({text: ''});
  } catch (error) { byId('status').textContent = chrome.i18n.getMessage('sendFailed') + ' ' + (error.message || ''); }
  finally { button.disabled = false; }
});

byId('send').disabled = false;
