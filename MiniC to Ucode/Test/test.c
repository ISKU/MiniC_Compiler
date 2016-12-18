int g1;
int g2;

int main() {
    int i = 1;
    int sum = 0;
    while(i < 12) {
        if(i == 10)
            break;
        if(i % 2 == 1) {
            ++i;
            continue;
        } else {
            write(i);
            ++i;
        }
    }
}