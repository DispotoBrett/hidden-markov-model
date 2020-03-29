cmake_minimum_required(VERSION 3.15)
project(HMM)

set(CMAKE_CXX_STANDARD 14)

add_executable(HMM main.cpp HiddenMarkovModel.cpp HiddenMarkovModel.h)