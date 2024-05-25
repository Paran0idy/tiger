#ifndef GC_H
#define GC_H

#include <stdio.h>
#include <stdlib.h>

void *Tiger_new(long size, void *vtable);
void *Tiger_newIntArray(int length);

#endif
