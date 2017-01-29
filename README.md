# MiniC to UCode Compiler
### About project
This is a MiniC compiler project using JAVA Antlr which generates UCode of IR.

### Contents
#### Simple pointer function
``` C
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
```

#### Compile error detection
> - Use of undeclared identifier<br>
> - Non-void function should return a value<br>
> - Void function should not return a value<br>
> - Initializing 'int' with an expression of incompatible type 'void'<br>

#### Make control flow graph
<img src="https://s30.postimg.org/5gb15pvqp/Screen_Shot_2017_01_29_at_11_25_14_AM.png" height="450">
<img src="https://s28.postimg.org/506wxmt0d/cfg.png" height="450">

#### Optimize unreachable code elimination(with Mark & Sweep Algorithm)
<img src="https://s29.postimg.org/4a8qk7o6f/op1.png" width="600">
<img src="https://s29.postimg.org/f84a9ygrr/op2.png" width="600">

### Author
[Minho Kim](https://github.com/ISKU), [Yoonjae Cho](https://github.com/Yoon-jae)
