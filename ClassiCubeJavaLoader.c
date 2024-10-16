#ifdef _WIN32
#define CC_API __declspec(dllimport)
#define CC_VAR __declspec(dllimport)
#define EXPORT __declspec(dllexport)
#else
#define CC_API
#define CC_VAR
#define EXPORT __attribute__((visibility("default")))
#endif

#include <math.h>

#include <windows.h>
#include <jni.h>

#include "..\src\Chat.h"
#include "..\src\Game.h"
#include "..\src\String.h"
#include "..\src\Event.h"

#define JLMOD "ClassiCubeJavaLoaderBridge.jar"
#define JVMDLL "bin\\server\\jvm.dll"
#define SHOULD_LOAD_VCRUNTIME true
#define VCRUNTIME "bin\\vcruntime140.dll"
#define SHOULD_LOAD_MSVCP true
#define MSVCP "bin\\msvcp140.dll"
#define STR_BUFFER_LENGTH 1024
#define LOADER_TICK_FREQUENCY 144
#define LOADER_TICK_EVENT_ID 0
#define SPECIAL_EVENTS_OFFSET 10000
#define SCHEDULED_TASKS_OFFSET 20000

typedef jint(JNICALL* CreateJavaVM)(JavaVM**, void**, void*);

static JavaVM* jvm = NULL;
static JNIEnv* env = NULL;
static jclass mainClass = NULL;

#pragma region Utils

BOOL FileExists(LPCSTR path) {
    DWORD attrubutes = GetFileAttributesA(path);

    return (attrubutes != INVALID_FILE_ATTRIBUTES && !(attrubutes & FILE_ATTRIBUTE_DIRECTORY));
}

static char* Concatenate(const char* first, const char* second) {
    size_t newLength = strlen(first) + strlen(second) + 1;
    char* newStr = (char*)malloc(newLength);
    if (newStr == NULL) return NULL;

    strcpy_s(newStr, newLength, first);
    strcat_s(newStr, newLength, second);
    // newStr[newLength - 1] = 0; // should not be needed: strcat is expected to put the terminator

    return newStr;
}

static cc_bool LoadJavaLibrary(const char* javaHomePath, cc_bool jre, const char* path) {
    char* relativePath = (jre ? Concatenate("jre\\", path) : path);
    char* libraryPath = Concatenate(javaHomePath, relativePath);
    HINSTANCE hModule = LoadLibraryA(libraryPath);
    free(libraryPath);
    if (jre) free(relativePath);

    return hModule != NULL;
}

static jmethodID GetStaticMethodId(const char* name, const char* signature) {
    return (*env)->GetStaticMethodID(env, mainClass, name, signature);
}

static jobject CallStaticObjectJava(const char* name, const char* signature) {
    jmethodID method = GetStaticMethodId(name, signature);
    if (method == NULL) return NULL;

    return (*env)->CallStaticObjectMethod(env, mainClass, method);
}

static void FireEvent(void* obj) {
    jint eventId = (jint) obj;
    jmethodID fireEvent = (*env)->GetStaticMethodID(env, mainClass, "fireEvent", "(I)V");
    if (fireEvent == NULL) return;
    (*env)->CallStaticVoidMethod(env, mainClass, fireEvent, eventId);
}

#pragma endregion Utils

static void PerformScheduledTask(struct ScheduledTask* task) {
    long withTaskId = (long)round(task->interval * 10000000.0);
    jint taskId = (jint) (withTaskId % 1000L);

    FireEvent(SCHEDULED_TASKS_OFFSET + taskId);
}

static void LoaderTick(struct ScheduledTask* task) {
    FireEvent(LOADER_TICK_EVENT_ID);

    jobjectArray chatMessages = CallStaticObjectJava("getPendingChatMessages", "()[Ljava/lang/String;");
    if (chatMessages == NULL) return;

    jsize messageCount = (*env)->GetArrayLength(env, chatMessages);
    for (jsize i = 0; i < messageCount; i++) {
        jstring string = (*env)->GetObjectArrayElement(env, chatMessages, i);
        const char* rawString = (*env)->GetStringUTFChars(env, string, 0);

        cc_string message = String_FromReadonly(rawString);
        Chat_Add(&message);

        (*env)->ReleaseStringUTFChars(env, string, rawString);
    }

    jintArray scheduledTaskIDs = CallStaticObjectJava("getPendingScheduledTaskIDs", "()[I");
    if (scheduledTaskIDs == NULL) return;

    jsize pendingCount = (*env)->GetArrayLength(env, scheduledTaskIDs);

    jint* taskIDs = (*env)->GetIntArrayElements(env, scheduledTaskIDs, 0);
    for (jsize i = 0; i < pendingCount; i++) {
        jint taskId = taskIDs[i];

        jmethodID method = GetStaticMethodId("getPendingScheduledTaskInterval", "(I)D");
        if (method == NULL) return;
        jdouble taskInterval = (*env)->CallStaticDoubleMethod(env, mainClass, method);

        double newInterval = taskInterval + (0.0000001 * taskId);
        ScheduledTask_Add(newInterval, PerformScheduledTask);
    }

    CallStaticObjectJava("freePendingInfo", "()Ljava.lang.Object;");
    (*env)->ReleaseIntArrayElements(env, scheduledTaskIDs, taskIDs, JNI_ABORT);

    /*
    // todo research why this code didn't work (the game likely crashed on GetDoubleArrayElements call)

    jdoubleArray scheduledTaskIntervals = CallStaticObjectJava("getPendingScheduledTaskIntervals", "()[D");
    if (scheduledTaskIntervals = NULL) return;

    jsize pendingCount = (*env)->GetArrayLength(env, scheduledTaskIDs);

    jint* taskIDs = (*env)->GetIntArrayElements(env, scheduledTaskIDs, 0);
    jdouble* taskIntervals = (*env)->GetDoubleArrayElements(env, scheduledTaskIntervals, 0);
    if (true) return;
    for (jsize i = 0; i < pendingCount; i++) {
        jint taskId = taskIDs[i];
        jdouble taskInterval = taskIntervals[i];

        double newInterval = taskInterval + (0.0000001 * taskId);
        ScheduledTask_Add(newInterval, PerformScheduledTask);
    }

    (*env)->ReleaseIntArrayElements(env, scheduledTaskIDs, taskIDs, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, scheduledTaskIntervals, taskIntervals, JNI_ABORT);
    */
}

static void SetupEvents() {
    if (EventAPIVersion < 4) {
        cc_string eventIssueMsg = String_FromConst("&cFailed to setup events: event API version is less than 4");
        Chat_Add(&eventIssueMsg);

        return;
    }
    jint eventId = LOADER_TICK_EVENT_ID + 1;

    // automatically generated code
    Event_Register(&EntityEvents.Added, eventId++, FireEvent);
    Event_Register(&EntityEvents.Removed, eventId++, FireEvent);
    Event_Register(&TabListEvents.Added, eventId++, FireEvent);
    Event_Register(&TabListEvents.Changed, eventId++, FireEvent);
    Event_Register(&TabListEvents.Removed, eventId++, FireEvent);
    Event_Register(&TextureEvents.AtlasChanged, eventId++, FireEvent);
    Event_Register(&TextureEvents.PackChanged, eventId++, FireEvent);
    Event_Register(&TextureEvents.FileChanged, eventId++, FireEvent);
    Event_Register(&GfxEvents.ViewDistanceChanged, eventId++, FireEvent);
    Event_Register(&GfxEvents.LowVRAMDetected, eventId++, FireEvent);
    Event_Register(&GfxEvents.ProjectionChanged, eventId++, FireEvent);
    Event_Register(&GfxEvents.ContextLost, eventId++, FireEvent);
    Event_Register(&GfxEvents.ContextRecreated, eventId++, FireEvent);
    Event_Register(&UserEvents.BlockChanged, eventId++, FireEvent);
    Event_Register(&UserEvents.HackPermsChanged, eventId++, FireEvent);
    Event_Register(&UserEvents.HeldBlockChanged, eventId++, FireEvent);
    Event_Register(&UserEvents.HacksStateChanged, eventId++, FireEvent);
    Event_Register(&BlockEvents.PermissionsChanged, eventId++, FireEvent);
    Event_Register(&BlockEvents.BlockDefChanged, eventId++, FireEvent);
    Event_Register(&WorldEvents.NewMap, eventId++, FireEvent);
    Event_Register(&WorldEvents.Loading, eventId++, FireEvent);
    Event_Register(&WorldEvents.MapLoaded, eventId++, FireEvent);
    Event_Register(&WorldEvents.EnvVarChanged, eventId++, FireEvent);
    Event_Register(&WorldEvents.LightingModeChanged, eventId++, FireEvent);
    Event_Register(&ChatEvents.FontChanged, eventId++, FireEvent);
    Event_Register(&ChatEvents.ChatReceived, eventId++, FireEvent);
    Event_Register(&ChatEvents.ChatSending, eventId++, FireEvent);
    Event_Register(&ChatEvents.ColCodeChanged, eventId++, FireEvent);
    Event_Register(&WindowEvents.RedrawNeeded, eventId++, FireEvent);
    Event_Register(&WindowEvents.Resized, eventId++, FireEvent);
    Event_Register(&WindowEvents.Closing, eventId++, FireEvent);
    Event_Register(&WindowEvents.FocusChanged, eventId++, FireEvent);
    Event_Register(&WindowEvents.StateChanged, eventId++, FireEvent);
    Event_Register(&WindowEvents.Created, eventId++, FireEvent);
    Event_Register(&WindowEvents.InactiveChanged, eventId++, FireEvent);
    Event_Register(&WindowEvents.Redrawing, eventId++, FireEvent);
    Event_Register(&InputEvents.Press, eventId++, FireEvent);
    Event_Register(&InputEvents._down, eventId++, FireEvent);
    Event_Register(&InputEvents._up, eventId++, FireEvent);
    Event_Register(&InputEvents.Wheel, eventId++, FireEvent);
    Event_Register(&InputEvents.TextChanged, eventId++, FireEvent);
    Event_Register(&InputEvents.Down2, eventId++, FireEvent);
    Event_Register(&InputEvents.Up2, eventId++, FireEvent);
    Event_Register(&PointerEvents.Moved, eventId++, FireEvent);
    Event_Register(&PointerEvents.Down, eventId++, FireEvent);
    Event_Register(&PointerEvents.Up, eventId++, FireEvent);
    Event_Register(&PointerEvents.RawMoved, eventId++, FireEvent);
    Event_Register(&ControllerEvents.AxisUpdate, eventId++, FireEvent);
    Event_Register(&NetEvents.Connected, eventId++, FireEvent);
    Event_Register(&NetEvents.Disconnected, eventId++, FireEvent);
    Event_Register(&NetEvents.PluginMessageReceived, eventId++, FireEvent);
}

static void ClassiCubeJavaLoader_Init() {
    cc_string initMsg = String_FromConst("ClassiCubeJavaLoader is initializing");
    Chat_Add(&initMsg);

    const char* javaHomePath = getenv("JAVA_HOME");
    if (javaHomePath == NULL) {
        cc_string javaHomeNotFoundMsg = String_FromConst("&c%JAVA_HOME% variable is not defined");
        Chat_Add(&javaHomeNotFoundMsg);

        return;
    }
    cc_bool jre = false;
    char* jvmDllPath = Concatenate(javaHomePath, JVMDLL);
    if (!FileExists(jvmDllPath)) {
        free(jvmDllPath);
        jre = true;
        jvmDllPath = Concatenate(javaHomePath, "jre\\" JVMDLL);
    }
    if (!FileExists(jvmDllPath)) {
        free(jvmDllPath);

        cc_string notFoundMsg = String_FromConst("&cFailed to locate jvm.dll in %JAVA_HOME%");
        Chat_Add(&notFoundMsg);

        return;
    }

    if (SHOULD_LOAD_VCRUNTIME && !LoadJavaLibrary(javaHomePath, jre, VCRUNTIME)) {
        cc_string failedToLoadMsg = String_FromConst("&cFailed to load [\\jre]" VCRUNTIME " from %JAVA_HOME%");
        Chat_Add(&failedToLoadMsg);

        return;
    }
    if (SHOULD_LOAD_MSVCP && !LoadJavaLibrary(javaHomePath, jre, MSVCP)) {
        cc_string failedToLoadMsg = String_FromConst("Failed to load [\\jre]" MSVCP " from %JAVA_HOME%");
        Chat_Add(&failedToLoadMsg);

        return;
    }
    
    HINSTANCE hModule = LoadLibraryA(jvmDllPath);
    free(jvmDllPath);

    if (!hModule) {
        cc_string failedToLoadMsg = String_FromConst("&cFailed to load [\\jre]" JVMDLL " from %JAVA_HOME%");
        Chat_Add(&failedToLoadMsg);

        return;
    }
    CreateJavaVM createJavaVM = (CreateJavaVM)GetProcAddress(hModule, "JNI_CreateJavaVM");

    JavaVMInitArgs vm_args;
    JavaVMOption* options = malloc(sizeof(JavaVMOption) * 2);
    if (options == NULL) {
        cc_string allocationFailureMsg = String_FromConst("&cFailed to allocate memory for JavaVMOption structures");
        Chat_Add(&allocationFailureMsg);

        return;
    }
    options[0].optionString = "-Djava.class.path=" JLMOD;
    options[1].optionString = "-Dfile.encoding=UTF8";
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;

    jint jvmResult = createJavaVM(&jvm, (void**)&env, &vm_args);
    free(options);

    if (jvmResult != JNI_OK) {
        cc_string failedToStartMsg = String_FromConst("&cFailed to start JavaVM");
        Chat_Add(&failedToStartMsg);

        return;
    }
    cc_string jvmOk = String_FromConst("JavaVM started");
    Chat_Add(&jvmOk);

    mainClass = (*env)->FindClass(env, "ccjl/Interface");
    jmethodID startMethod = (*env)->GetStaticMethodID(env, mainClass, "start", "()Z");

    jboolean bridgeResult = (*env)->CallStaticBooleanMethod(env, mainClass, startMethod);
    if (!bridgeResult) {
        cc_string failedToStartMsg = String_FromConst("&cFailed to start bridge");
        Chat_Add(&failedToStartMsg);

        return;
    }
    cc_string bridgeOk = String_FromConst("Bridge started");
    Chat_Add(&bridgeOk);

    SetupEvents();

    ScheduledTask_Add(1.0 / LOADER_TICK_FREQUENCY, LoaderTick);
}

static void ClassiCubeJavaLoader_Free() {
    FireEvent(SPECIAL_EVENTS_OFFSET);

    (*jvm)->DestroyJavaVM(jvm);
}

static void ClassiCubeJavaLoader_Reset() {
    FireEvent(SPECIAL_EVENTS_OFFSET + 1);
}

static void ClassiCubeJavaLoader_OnNewMap() {
    FireEvent(SPECIAL_EVENTS_OFFSET + 2);
}

static void ClassiCubeJavaLoader_OnNewMapLoaded() {
    FireEvent(SPECIAL_EVENTS_OFFSET + 3);
}

EXPORT int Plugin_ApiVersion = 1;
EXPORT struct IGameComponent Plugin_Component = {
    ClassiCubeJavaLoader_Init,
    ClassiCubeJavaLoader_Free,
    ClassiCubeJavaLoader_Reset,
    ClassiCubeJavaLoader_OnNewMap,
    ClassiCubeJavaLoader_OnNewMapLoaded
};
