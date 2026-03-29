const apkStatus = document.getElementById("apk-status");

fetch("/downloads/MirrorAir-debug.apk", { method: "HEAD" })
  .then((response) => {
    if (response.ok) {
      apkStatus.textContent = "APK status: ready for download.";
      return;
    }
    throw new Error("APK not found");
  })
  .catch(() => {
    apkStatus.textContent = "APK status: no APK uploaded yet. Build the app and copy it into downloads/.";
  });
