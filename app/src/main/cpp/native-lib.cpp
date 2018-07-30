#include <jni.h>
#include <string>
#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sstream>
#include <android/log.h>

using namespace std;


extern "C"
JNIEXPORT jint JNICALL
Java_io_appetizer_anti_1xposed_antiXposed_checkMaps(JNIEnv *env, jobject instance) {

    stringstream strStream;
    int pid = (int)getpid();
    strStream << pid;
    string s_pid = strStream.str();
    string filePath = "/proc/" + s_pid + "/maps";
    ifstream fin(filePath.c_str());
    string s;
    while( getline(fin,s) )
    {
        if(s.find("XposedBridge.jar")!= string::npos){
            //__android_log_print(ANDROID_LOG_INFO, "System.out","%s", s_pid.c_str());
            return -5;
        }
    }
    return 0;
}