#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <jni.h>
#include <signal.h>


#define WIN32_LEAN_AND_MEAN
#define NOMSG
#include <windows.h>
#include <process.h>
#include <winsock2.h>

#define jlong  __int64

#define FT2INT64(ft) ((jlong)(ft).dwHighDateTime << 32 | (jlong)(ft).dwLowDateTime)


/* This standard C function is not provided on Windows */
char* strsignal(int signum) {
    switch (signum) {
        case SIGABRT:     return "SIGABRT: Abnormal termination";
        case SIGFPE:      return "SIGFPE: Floating-point error";
        case SIGILL:      return "SIGILL: Illegal instruction";
        case SIGINT:      return "SIGINT: CTRL+C signal";
        case SIGSEGV:     return "SIGSEGV: Illegal storage access";
        case SIGTERM:     return "SIGTERM: Termination request";
        default:          return "<unknown signal>";
    }
}


jlong sysTimeMillis(void) {
    static jlong fileTime_1_1_70 = 0;
    SYSTEMTIME st0;
    FILETIME   ft0;

    if (fileTime_1_1_70 == 0) {
        /*
         * Initialize fileTime_1_1_70 -- the Win32 file time of midnight
         * 1/1/70.
         */
        memset(&st0, 0, sizeof(st0));
        st0.wYear  = 1970;
        st0.wMonth = 1;
        st0.wDay   = 1;
        SystemTimeToFileTime(&st0, &ft0);
        fileTime_1_1_70 = FT2INT64(ft0);
    }

    GetSystemTime(&st0);
    SystemTimeToFileTime(&st0, &ft0);

    /* file times are in 100ns increments, i.e. .0001ms */
    return (FT2INT64(ft0) - fileTime_1_1_70) / 10000;
}



jint createJVM(JavaVM **jvm, void **env, void *args) {
    HINSTANCE handle;
    jint (JNICALL *CreateJavaVM)(JavaVM **jvm, void **env, void *args) = 0;

/*
    char *name = "jni.dll";
    FILE *f = fopen("jvmdll.dat", "r");

    if (f != 0) {
        char jreDll[256];
        int i = 0;
        int ch;
        while((ch = getc(f)) > ' ' ) {
            jreDll[i++] = ch;
        }
        jreDll[i++] = 0;
        name = jreDll;
    }
*/


    char *name = getenv("JVMDLL");
    if (name == 0) {
        name = "jvm.dll";
    }


    handle = LoadLibrary(name);
    if (handle == 0) {
        fprintf(stderr, "Cannot load %s\n", name);
        fprintf(stderr, "Please add the directory containing jvm.dll to your PATH\n");
        fprintf(stderr, "environment variable or set the JVMDLL environment variable\n");
        fprintf(stderr, "to the full path of this file.\n");
        exit(1);
    }

    CreateJavaVM = (jint (JNICALL *)(JavaVM **,void **, void *)) GetProcAddress(handle, "JNI_CreateJavaVM");

    if (CreateJavaVM == 0) {
        fprintf(stderr,"Cannot resolve JNI_CreateJavaVM in %s\n", name);
        exit(1);
    }

    return CreateJavaVM(jvm, env, args);
}





int sleepTime;
extern int ticks;

static void ticker(void) {
    for(;;) {
        Sleep(sleepTime);
        ticks++;
    }
}

void startTicker(int interval) {
    sleepTime = interval;
#ifdef _MT
    _beginthread((void (*))ticker, 0, 0);
#else
    fprintf(stderr, "Profiling not implemented");
    exit(0);
#endif
}

