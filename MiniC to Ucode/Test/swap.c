void swap(int *a, int *b) {
   int temp;
   temp = *a;
   *a = *b;
   *b = temp;
}

void main() {
   int a = 5;
   int b = 10;
   swap(&a, &b);
   write(a);
   write(b);
}