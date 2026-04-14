const MENU_ID = "translate-to-darija";

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_ID,
      title: "Translate to Darija",
      contexts: ["selection"]
    });
  });
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== MENU_ID || !tab?.id) return;

  const selectedText = (info.selectionText || "").trim();
  if (!selectedText) return;

  try {
    await chrome.storage.local.set({
      selectedText,
      autoTranslate: true
    });

    await chrome.sidePanel.setOptions({
      tabId: tab.id,
      path: "sidepanel.html",
      enabled: true
    });

    await chrome.sidePanel.open({ tabId: tab.id });

    // If panel is already open, this updates instantly.
    chrome.runtime.sendMessage({
      type: "SET_TEXT",
      text: selectedText,
      autoTranslate: true
    });
  } catch (error) {
    console.error("Failed to open side panel:", error);
  }
});
