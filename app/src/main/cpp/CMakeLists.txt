cmake_minimum_required(VERSION 3.22.1)

project(mirrornode_receiver)

add_library(
    mirrornode_receiver
    SHARED
    openairplay_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    mirrornode_receiver
    ${log-lib}
    ${android-lib}
)
