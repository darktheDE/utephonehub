# utephonehub
ĐỒ ÁN MÔN LẬP TRÌNH WEB, HCMUTE

## Tech Stack
- Frontend: React + Vite + TailwindCSS + shadcn/ui
- Backend: Java Servlet + JPA (Hibernate)
- Database: PostgreSQL
- Build: Maven (backend), npm (frontend)

## Hướng dẫn chạy project

### Yêu cầu
- Java 17
- Docker & Docker Compose
- Node.js

## Git Workflow

### Setup ban đầu
```
git clone https://github.com/darktheDE/utephonehub
cd utephonehub
git checkout develop
```

### Quy trình làm việc hàng ngày
```
# 1. Cập nhật code mới nhất
git checkout develop
git pull origin develop

# 2. Tạo nhánh feature mới
git checkout -b feature/Mxx-ten-task

# 3. Code và commit
git add .
git commit -m "feat(Mxx): mô tả công việc"

# 4. Push và tạo Pull Request
git push --set-upstream origin feature/Mxx-ten-task
```

### Quy ước
- Tên nhánh: `feature/Mxx-ten-task` hoặc `fix/Mxx-ten-loi`
- Commit: `feat(Mxx): mô tả` hoặc `fix(Mxx): mô tả`
- Chưa xong: `WIP(Mxx): mô tả`
- Không push trực tiếp lên `main` hoặc `develop`

### 1. Chạy database (PostgreSQL)
```
cd server/utephonehub
docker-compose -f postgres.yml up -d
```
Database chạy tại localhost:5434
- User: utephonehub
- Password: utephonehub123
- Database: utephonehub

### 2. Chạy backend (Java Servlet)
```
cd server/utephonehub
./mvnw compile exec:java
```
Backend chạy tại http://localhost:8080

### 3. Chạy frontend (React + Vite)
```
cd client
npm install
npm run dev
```
Frontend chạy tại http://localhost:3000