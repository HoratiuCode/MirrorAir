if(NOT TARGET openssl::crypto)
add_library(openssl::crypto SHARED IMPORTED)
set_target_properties(openssl::crypto PROPERTIES
    IMPORTED_LOCATION "/Users/horatiubudai/.gradle/caches/9.4.0/transforms/aac0911ec9c321795a9c9f0e21e66f77/transformed/openssl-1.1.1q-beta-1/prefab/modules/crypto/libs/android.x86_64/libcrypto.so"
    INTERFACE_INCLUDE_DIRECTORIES "/Users/horatiubudai/.gradle/caches/9.4.0/transforms/aac0911ec9c321795a9c9f0e21e66f77/transformed/openssl-1.1.1q-beta-1/prefab/modules/crypto/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

if(NOT TARGET openssl::ssl)
add_library(openssl::ssl SHARED IMPORTED)
set_target_properties(openssl::ssl PROPERTIES
    IMPORTED_LOCATION "/Users/horatiubudai/.gradle/caches/9.4.0/transforms/aac0911ec9c321795a9c9f0e21e66f77/transformed/openssl-1.1.1q-beta-1/prefab/modules/ssl/libs/android.x86_64/libssl.so"
    INTERFACE_INCLUDE_DIRECTORIES "/Users/horatiubudai/.gradle/caches/9.4.0/transforms/aac0911ec9c321795a9c9f0e21e66f77/transformed/openssl-1.1.1q-beta-1/prefab/modules/ssl/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

