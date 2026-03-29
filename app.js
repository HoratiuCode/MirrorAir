const apkStatus = document.getElementById("apk-status");
const downloadButton = document.getElementById("downloadButton");

fetch("/downloads/MirrorAir-debug.apk", { method: "HEAD" })
  .then((response) => {
    if (response.ok) {
      apkStatus.textContent = "APK status: ready for download.";
      downloadButton.textContent = "Download APK";
      downloadButton.href = "/downloads/MirrorAir-debug.apk";
      downloadButton.removeAttribute("aria-disabled");
      downloadButton.classList.remove("button-disabled");
      return;
    }
    throw new Error("APK not found");
  })
  .catch(() => {
    apkStatus.textContent = "APK status: no APK uploaded yet. Build the app and copy it into downloads/.";
    downloadButton.textContent = "APK Coming Soon";
    downloadButton.href = "#";
    downloadButton.setAttribute("aria-disabled", "true");
    downloadButton.classList.add("button-disabled");
  });
