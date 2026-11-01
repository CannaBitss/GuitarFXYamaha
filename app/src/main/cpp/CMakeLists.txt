# CMakeLists.txt for GuitarFX Pro Native Audio Engine
cmake_minimum_required(VERSION 3.22.1)
project("guitar_fx_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable aggressive math optimizations for DSP real-time performance
add_compile_options(-O3 -ffast-math -flto)

# In a full NDK build, oboe can be added via FetchContent or find_package:
# find_package(oboe REQUIRED CONFIG)

add_library(guitar_fx_engine SHARED
    OboeAudioEngine.cpp
    JniBridge.cpp
)

find_library(log-lib log)
find_library(aaudio-lib aaudio)

target_link_libraries(guitar_fx_engine
    ${log-lib}
    ${aaudio-lib}
    # oboe::oboe
)
