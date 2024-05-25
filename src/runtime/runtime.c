#include <stdio.h>
#include <stdlib.h>
#include "todo.h"

extern void Tiger_main();

void *Tiger_new(long size, void *vtable){
    TODO();
    return (void *)0;
}

void *Tiger_getVirtualMethod(long *ptr, long vtableOffset, long methodOffset){
    TODO();
    return (void *)0;
}

void Tiger_print(long n){
    printf("%ld\n", n);
}

void Tiger_debug(char *s){
    printf("%s\n", s);
}

int main(int argc, char **argv){
    Tiger_main();
    return 0;
}

