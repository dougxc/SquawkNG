#include <stdlib.h>
#include <sys/time.h>
#include <dlfcn.h>
#include <jni.h>

#define jlong  int64_t

jlong sysTimeMillis() {
    struct timeval tv;
    long long result;
    gettimeofday(&tv, NULL);
    /* We adjust to 1000 ticks per second */
    result = (jlong)tv.tv_sec * 1000 + tv.tv_usec/1000;
    return result;
}

jint createJVM(JavaVM **jvm, void **env, void *args) {
    jint (JNICALL *CreateJavaVM)(JavaVM **jvm, void **env, void *args) = 0;
    const char* name = "libjvm.so";
    void* libVM = dlopen(name, RTLD_LAZY);
    if (libVM == 0) {
        fprintf(stderr, "Cannot load %s\n", name);
        fprintf(stderr, "Please add the directories containing libjvm.so and libverify.so\n");
        fprintf(stderr, "to the LD_LIBRARY_PATH environment variable.\n");
        exit(1);
    }

    CreateJavaVM = (jint (JNICALL *)(JavaVM **,void **, void *)) dlsym(libVM, "JNI_CreateJavaVM");

    if (CreateJavaVM == 0) {
        fprintf(stderr,"Cannot resolve JNI_CreateJavaVM in %s\n", name);
        exit(1);
    }

    return CreateJavaVM(jvm, env, args);
}


void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

