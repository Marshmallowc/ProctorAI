import sys
import json
def main():
    # 从标准输入读取两个列表
    list1 = json.loads(sys.stdin.readline())  # 解析 JSON 字符串
    list2 = json.loads(sys.stdin.readline())  # 解析 JSON 字符串
    # 打印两个列表
    print("Received List 1:", list1)
    print("Received List 2:", list2)
    # 返回一个浮动类型的0.5
    return_value = 0.5
    print("Returning:", return_value)
    # 返回 0.5 给 Java
    sys.stdout.write(str(return_value))
if __name__ == "__main__":
    main()
