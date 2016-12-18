int cache[100];

void main() {
    int result;
    memset(cache, -1, 100);
    cache[0] = 0;
    cache[1] = 1;
    cache[2] = 1;
    result = fibo(30);
    write(result);
}

int fibo(int n) {
    if(cache[n] != -1)
        return cache[n];
    cache[n] = fibo(n-1) + fibo(n-2);
    return cache[n];
}

void memset(int array[], int value, int size) {
    int i = 0;
    while(i < size) {
        array[i] = value;
        ++i;
    }
}