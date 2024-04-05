int main(){
    SumRec_main();
}

void *Tiger_new(long size, void *vtable){
    void *ptr = malloc(size);
    *ptr = vtable;
    return ptr;
}

void Tiger_print(long n){
    printf("%ld\n", n);
}
