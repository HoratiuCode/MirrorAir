const apkStatus = document.getElementById("apk-status");
const downloadButton = document.getElementById("downloadButton");
const apkPath = "./downloads/MirrorAir-1.0.1.apk";

fetch(apkPath, { method: "HEAD" })
  .then((response) => {
    if (response.ok) {
      apkStatus.textContent = "APK status: latest experimental build is ready for download.";
      downloadButton.textContent = "Download APK";
      downloadButton.href = apkPath;
      downloadButton.setAttribute("download", "");
      downloadButton.removeAttribute("aria-disabled");
      downloadButton.classList.remove("button-disabled");
      return;
    }
    throw new Error("APK not found");
  })
  .catch(() => {
    apkStatus.textContent = "APK status: no APK uploaded yet. Build the app and copy MirrorAir-1.0.1.apk into docs/downloads/.";
    downloadButton.textContent = "APK Unavailable";
    downloadButton.href = "#";
    downloadButton.setAttribute("aria-disabled", "true");
    downloadButton.classList.add("button-disabled");
  });
