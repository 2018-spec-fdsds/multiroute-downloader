import {sendUrl} from './client.js';

function menus() {
  chrome.contextMenus.removeAll(() => chrome.contextMenus.create({id: 'multiroute-download', title: chrome.i18n.getMessage('downloadLink'), contexts: ['link']}));
}
chrome.runtime.onInstalled.addListener(menus);
chrome.runtime.onStartup.addListener(menus);
chrome.contextMenus.onClicked.addListener(async info => {
  if (info.menuItemId !== 'multiroute-download') return;
  try {
    await sendUrl(info.linkUrl);
    await chrome.storage.local.remove('lastError');
    await chrome.action.setBadgeBackgroundColor({color: '#167d65'});
    await chrome.action.setBadgeText({text: '✓'});
  } catch (error) {
    await chrome.storage.local.set({lastError: chrome.i18n.getMessage('sendFailed') + ' ' + (error.message || '')});
    await chrome.action.setBadgeBackgroundColor({color: '#b12634'});
    await chrome.action.setBadgeText({text: '!'});
  }
});
