#include <stdio.h>
#include <stdlib.h>

extern void Tiger_main();

void *Tiger_new(long size, void *vtable){
    void **ptr = (void **)malloc(size);
    *ptr = vtable;
    return (void *)ptr;
}

void *Tiger_getVirtualMethod(long *ptr, long vtableOffset, long methodOffset){
    long *vtable = (long *)ptr[vtableOffset];
    long *method = (long *)vtable[methodOffset];
    return method;
}

void Tiger_print(long n){
    printf("%ld\n", n);
}

void Tiger_debug(char *s){
    printf("%s\n", s);
}


int main(){
    Tiger_main();
    return 0;
}

