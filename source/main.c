#include <stdio.h>
#include "reciprocal.hpp"
int atoi(char * i);
int main (int argc, char **argv)
{
 int i; 
 if (argc < 2) 
   {
     printf("Need a value to evaluate\n");
     return 1;
   }
 i = atoi(argv[1]);
 printf ("The reciprocal of %d is %f\n",i,reciprocal(i));
 return 0;
}



