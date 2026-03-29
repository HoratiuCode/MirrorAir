const apkStatus = document.getElementById("apk-status");
const downloadButton = document.getElementById("downloadButton");

fetch("/downloads/MirrorAir-Mac-Helper.zip", { method: "HEAD" })
  .then((response) => {
    if (response.ok) {
      apkStatus.textContent = "Helper status: ready for download.";
      downloadButton.textContent = "Download Mac Helper";
      downloadButton.href = "/downloads/MirrorAir-Mac-Helper.zip";
      downloadButton.removeAttribute("aria-disabled");
      downloadButton.classList.remove("button-disabled");
      return;
    }
    throw new Error("Helper not found");
  })
  .catch(() => {
    apkStatus.textContent = "Helper status: package missing from downloads/.";
    downloadButton.textContent = "Helper Unavailable";
    downloadButton.href = "#";
    downloadButton.setAttribute("aria-disabled", "true");
    downloadButton.classList.add("button-disabled");
  });
