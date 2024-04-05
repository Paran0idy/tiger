#include <stdio.h>
#include <stdlib.h>

extern int SumRec_main();

void *Tiger_new(long size, void *vtable){
    void **ptr = (void **)malloc(size);
    *ptr = vtable;
    return ptr;
}

void Tiger_print(long n){
    printf("%ld\n", n);
}

int main(){
    SumRec_main();
    return 0;
}