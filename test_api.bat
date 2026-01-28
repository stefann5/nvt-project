@echo off
echo ============================================ > C:\homework\semstar7\nvt\nvt-project\api_results.txt
echo API ENDPOINT TEST RESULTS >> C:\homework\semstar7\nvt\nvt-project\api_results.txt
echo ============================================ >> C:\homework\semstar7\nvt\nvt-project\api_results.txt

echo. >> C:\homework\semstar7\nvt\nvt-project\api_results.txt
echo [1] Login Test >> C:\homework\semstar7\nvt\nvt-project\api_results.txt
curl -s -w "\nHTTP_CODE:%%{http_code}\nTIME:%%{time_total}s\n" -X POST "http://localhost:8080/api/v1/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"sifra\"}" -o C:\homework\semstar7\nvt\nvt-project\login_response.json >> C:\homework\semstar7\nvt\nvt-project\api_results.txt 2>&1
type C:\homework\semstar7\nvt\nvt-project\login_response.json >> C:\homework\semstar7\nvt\nvt-project\api_results.txt

echo TESTS STARTED >> C:\homework\semstar7\nvt\nvt-project\api_results.txt
