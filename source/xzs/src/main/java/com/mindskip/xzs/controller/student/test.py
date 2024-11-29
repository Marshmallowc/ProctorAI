import io
import sys
import cv2
import base64
import numpy as np
import face_recognition
import mysql.connector
def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    # 从标准输入中读取 Base64 编码的图像数据
    image_data = sys.stdin.read().strip()
    # 检查图像数据是否为空
    if not image_data:
        print("failed: No image data provided")
        return
    # 移除 "data:image/jpeg;base64," 等头部信息
    if ',' in image_data:
        image_data = image_data.split(',')[1]
    # 解码 Base64 数据
    try:
        image_bytes = base64.b64decode(image_data)
        nparr = np.frombuffer(image_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            print("failed: Decoded frame is None")
            return
    except Exception as e:
        print(f"failed: Error decoding image - {str(e)}")
        return
    # 连接到MySQL数据库
    try:
        db = mysql.connector.connect(
            host="localhost",
            user="root",
            password="123456",
            database="xzs"
        )
        cursor = db.cursor()
    except Exception as e:
        print(f"failed: Error connecting to the database - {str(e)}")
        return
    # 加载已知人脸数据从数据库中
    known_face_encodings = []
    known_face_names = []
    try:
        cursor.execute("SELECT face_name, encoding FROM t_face_encoding")
        results = cursor.fetchall()
        for row in results:
            face_name = row[0]
            encoding_blob = row[1]
            known_encoding = np.frombuffer(encoding_blob, dtype=np.float64)
            known_face_encodings.append(known_encoding)
            known_face_names.append(face_name)
    except Exception as e:
        print(f"failed: Error fetching data from database - {str(e)}")
        return
    # 转换为灰度图像（如果需要使用OpenCV进行处理）
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    # 使用 face_recognition 进行人脸检测和识别
    face_locations = face_recognition.face_locations(frame)
    face_encodings = face_recognition.face_encodings(frame, face_locations)
    if len(face_encodings) > 0:
        for face_encoding in face_encodings:
            matches = face_recognition.compare_faces(known_face_encodings, face_encoding)
            name = "Unknown"
            face_distances = face_recognition.face_distance(known_face_encodings, face_encoding)
            best_match_index = np.argmin(face_distances)
            if matches[best_match_index]:
                name = known_face_names[best_match_index]
            print(f"success/{name}")
    else:
        print("failed")
    # 关闭数据库连接
    cursor.close()
    db.close()
if __name__ == "__main__":
    main()
