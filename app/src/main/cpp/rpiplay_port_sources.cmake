set(MIRRORAIR_RPIPLAY_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/rpiplay")
set(MIRRORAIR_LIBPLIST_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/libplist")

set(MIRRORAIR_RPIPLAY_INCLUDE_DIRS
    "${MIRRORAIR_RPIPLAY_ROOT}"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/llhttp"
    "${MIRRORAIR_RPIPLAY_ROOT}/renderers"
    "${MIRRORAIR_RPIPLAY_ROOT}/android"
    "${MIRRORAIR_LIBPLIST_ROOT}/include"
    "${MIRRORAIR_LIBPLIST_ROOT}/src"
    "${MIRRORAIR_LIBPLIST_ROOT}/libcnary/include"
)

set(MIRRORAIR_RPIPLAY_CORE_SOURCES
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/byteutils.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/crypto.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/fairplay_playfair.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/http_request.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/http_response.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/httpd.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/logger.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/mirror_buffer.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/netutils.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/pairing.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/raop.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/raop_buffer.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/raop_ntp.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/raop_rtp.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/raop_rtp_mirror.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/utils.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair/hand_garble.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair/modified_md5.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair/omg_hax.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair/playfair.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/playfair/sap_hash.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/llhttp/api.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/llhttp/http.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/lib/llhttp/llhttp.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/android/dnssd_android_stub.c"
)

set(MIRRORAIR_RPIPLAY_DUMMY_RENDERER_SOURCES
    "${MIRRORAIR_RPIPLAY_ROOT}/renderers/audio_renderer_dummy.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/renderers/video_renderer_dummy.c"
    "${MIRRORAIR_RPIPLAY_ROOT}/android/video_renderer_android.c"
)

set(MIRRORAIR_LIBPLIST_SOURCES
    "${MIRRORAIR_LIBPLIST_ROOT}/libcnary/node.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/libcnary/node_list.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/base64.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/bplist.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/bytearray.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/hashtable.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/jplist.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/jsmn.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/oplist.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/out-default.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/out-limd.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/out-plutil.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/plist.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/ptrarray.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/time64.c"
    "${MIRRORAIR_LIBPLIST_ROOT}/src/xplist.c"
)

set(MIRRORAIR_RPIPLAY_ENTRY_SOURCE
    "${MIRRORAIR_RPIPLAY_ROOT}/rpiplay.cpp"
)

set(MIRRORAIR_ENABLE_RPIPLAY_PORT ON CACHE BOOL "Enable experimental Android RPiPlay port sources")
