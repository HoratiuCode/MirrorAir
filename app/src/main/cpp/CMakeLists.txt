cmake_minimum_required(VERSION 3.22.1)

project(mirrornode_receiver)

include(${CMAKE_CURRENT_LIST_DIR}/rpiplay_port_sources.cmake)

add_library(
    mirrornode_receiver
    SHARED
    openairplay_bridge.cpp
)

target_include_directories(
    mirrornode_receiver
    PRIVATE
    ${MIRRORAIR_RPIPLAY_INCLUDE_DIRS}
)

target_compile_definitions(
    mirrornode_receiver
    PRIVATE
    HAS_DUMMY_RENDERER
)

find_library(log-lib log)
find_library(android-lib android)

if(MIRRORAIR_ENABLE_RPIPLAY_PORT)
    find_package(openssl REQUIRED CONFIG)

    add_library(
        mirroraire_plist
        STATIC
        ${MIRRORAIR_LIBPLIST_SOURCES}
    )
    target_include_directories(
        mirroraire_plist
        PUBLIC
        ${MIRRORAIR_RPIPLAY_INCLUDE_DIRS}
    )
    target_compile_definitions(
        mirroraire_plist
        PUBLIC
        LIBPLIST_STATIC
        HAVE_STRNDUP
        PACKAGE_VERSION="2.7.0"
    )

    add_library(
        mirroraire_rpiplay
        STATIC
        ${MIRRORAIR_RPIPLAY_ENTRY_SOURCE}
        ${MIRRORAIR_RPIPLAY_CORE_SOURCES}
        ${MIRRORAIR_RPIPLAY_DUMMY_RENDERER_SOURCES}
    )
    target_include_directories(
        mirroraire_rpiplay
        PUBLIC
        ${MIRRORAIR_RPIPLAY_INCLUDE_DIRS}
    )
    target_compile_definitions(
        mirroraire_rpiplay
        PRIVATE
        MIRRORAIR_RPIPLAY_LIBRARY
        HAS_DUMMY_RENDERER
        HAS_ANDROID_RENDERER
        OPENSSL_API_COMPAT=0x10101000L
    )
    target_link_libraries(
        mirroraire_rpiplay
        PUBLIC
        mirroraire_plist
        openssl::crypto
        ${log-lib}
    )
endif()

target_link_libraries(
    mirrornode_receiver
    ${log-lib}
    ${android-lib}
)

if(MIRRORAIR_ENABLE_RPIPLAY_PORT)
    target_link_libraries(
        mirrornode_receiver
        mirroraire_rpiplay
    )
    message(STATUS "Experimental Android RPiPlay port source set enabled")
endif()
