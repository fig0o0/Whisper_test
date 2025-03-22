cmake_minimum_required(VERSION 3.10)

project(whisper_integration)

set(CMAKE_CXX_STANDARD 17)

# 使用GGML自带的CMake配置
add_subdirectory(${CMAKE_SOURCE_DIR}/whisper/ggml)

# 只保留whisper和jni相关的源文件
set(
    WHISPER_SOURCES
    ${CMAKE_SOURCE_DIR}/whisper/whisper.cpp
    ${CMAKE_SOURCE_DIR}/whisper/jni.c
)

# 查找日志库
find_library(LOG_LIB log)

# 根据不同的ABI添加不同的编译选项
function(build_library target_name)
    add_library(
        ${target_name}
        SHARED
        ${WHISPER_SOURCES}
    )

    if (${ANDROID_ABI} STREQUAL "arm64-v8a")
        target_compile_options(${target_name} PRIVATE -march=armv8.2-a+fp16)
    elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
        target_compile_options(${target_name} PRIVATE -mfpu=neon-vfpv4)
    endif()

    # 编译选项
    if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
        target_compile_options(${target_name} PRIVATE -O3)
        target_compile_options(${target_name} PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
        target_compile_options(${target_name} PRIVATE -ffunction-sections -fdata-sections)

        target_link_options(${target_name} PRIVATE -Wl,--gc-sections)
        target_link_options(${target_name} PRIVATE -Wl,--exclude-libs,ALL)
        target_link_options(${target_name} PRIVATE -flto)
    endif()

    # 定义预处理器宏以适配Android平台
    target_compile_definitions(${target_name} PRIVATE 
        GGML_USE_ACCELERATE=0
        GGML_USE_METAL=0
    )

    # 链接库 - 现在链接到ggml库
    target_link_libraries(${target_name} ${LOG_LIB} android ggml)
endfunction()

# 根据ABI创建特定优化的库
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    build_library("whisper_v8fp16_va")
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    build_library("whisper_vfpv4")
endif()

# 创建默认目标库
build_library("whisper")

# 添加包含目录
include_directories(${CMAKE_SOURCE_DIR})
include_directories(${CMAKE_SOURCE_DIR}/whisper)
include_directories(${CMAKE_SOURCE_DIR}/whisper/ggml/include)  # 使用GGML的include目录
include_directories(${CMAKE_SOURCE_DIR}/whisper/ggml/src)      # 使用GGML的src目录
