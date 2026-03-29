const apkStatus = document.getElementById("apk-status");
const downloadButton = document.getElementById("downloadButton");

fetch("/downloads/MirrorAir.apk", { method: "HEAD" })
  .then((response) => {
    if (response.ok) {
      apkStatus.textContent = "APK status: ready for download.";
      downloadButton.textContent = "Download APK";
      downloadButton.href = "/downloads/MirrorAir.apk";
      downloadButton.removeAttribute("aria-disabled");
      downloadButton.classList.remove("button-disabled");
      return;
    }
    throw new Error("APK not found");
  })
  .catch(() => {
    apkStatus.textContent = "APK status: package missing from downloads/.";
    downloadButton.textContent = "APK Unavailable";
    downloadButton.href = "#";
    downloadButton.setAttribute("aria-disabled", "true");
    downloadButton.classList.add("button-disabled");
  });
