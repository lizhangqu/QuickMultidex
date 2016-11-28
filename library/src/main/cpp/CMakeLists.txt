
cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/include/)

add_library(multidex
            SHARED
            multidex.c)


target_link_libraries(multidex
                      android
                      log)