const API_URL = "http://localhost:8080/api/translate";
const AUTH_HEADER = "Basic " + btoa("student:student123");

const sourceTextEl = document.getElementById("sourceText");
const resultEl = document.getElementById("result");
const statusEl = document.getElementById("status");
const translateBtn = document.getElementById("translateBtn");

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle("error", isError);
}

function setResult(text) {
  if (!resultEl) return;
  if ("value" in resultEl) {
    resultEl.value = text;
  } else {
    resultEl.textContent = text;
  }
}

async function translateCurrentText() {
  const text = sourceTextEl.value.trim();

  if (!text) {
    setStatus("Please enter or select text first.", true);
    setResult("");
    return;
  }

  translateBtn.disabled = true;
  setStatus("Translating...");
  setResult("");

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: AUTH_HEADER
      },
      body: JSON.stringify({ text })
    });

    const responseBody = await response.text();

    if (!response.ok) {
      throw new Error(responseBody || `HTTP ${response.status}`);
    }

    setResult(responseBody);
    setStatus("Translation complete.");
  } catch (error) {
    console.error(error);
    setResult("");
    setStatus(`Error: ${error.message}`, true);
  } finally {
    translateBtn.disabled = false;
  }
}

async function loadPendingSelection() {
  const { selectedText, autoTranslate } = await chrome.storage.local.get([
    "selectedText",
    "autoTranslate"
  ]);

  if (selectedText) {
    sourceTextEl.value = selectedText;
    if (autoTranslate) {
      await translateCurrentText();
    }
    await chrome.storage.local.set({ autoTranslate: false });
  }
}

translateBtn.addEventListener("click", translateCurrentText);

chrome.runtime.onMessage.addListener((message) => {
  if (message?.type !== "SET_TEXT") return;

  sourceTextEl.value = message.text || "";
  setResult("");

  if (message.autoTranslate) {
    translateCurrentText();
  }
});

loadPendingSelection();
