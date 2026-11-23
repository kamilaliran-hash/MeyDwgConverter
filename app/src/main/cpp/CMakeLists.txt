cmake_minimum_required(VERSION 3.22.1)
project(dwgconv C)

# Prebuilt static LibreDWG, produced by scripts/build-libredwg.sh
set(LIBREDWG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libredwg/${ANDROID_ABI})

if(NOT EXISTS ${LIBREDWG_DIR}/lib/libredwg.a)
  message(FATAL_ERROR
    "LibreDWG for ${ANDROID_ABI} not found in ${LIBREDWG_DIR}.\n"
    "Run scripts/build-libredwg.sh first (see README.md).")
endif()

add_library(redwg STATIC IMPORTED)
set_target_properties(redwg PROPERTIES
  IMPORTED_LOCATION ${LIBREDWG_DIR}/lib/libredwg.a)

add_library(dwgconv SHARED dwgconv.c)
target_include_directories(dwgconv PRIVATE ${LIBREDWG_DIR}/include)
target_link_libraries(dwgconv redwg log m)
