package org.example.demo2.service;

import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.demo2.dto.Result;
import org.example.demo2.dto.SimData;
import org.example.demo2.dto.SimRequest;
import org.example.demo2.dto.SimResponse;
import org.example.demo2.dto.SimResult;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SimService {
    public void getAllSimWithHighPoint(List<Integer> pointElement) {
        List<SimResult> listWebScore = new ArrayList<>();

        List<String> dataViettel = getDataViettel();

        int f = 0;
        //Lấy sim theo hệ
        List<SimResult> element = getElement(dataViettel, pointElement);

        //Lấy sim theo điểm 80
        List<SimResult> goodNumber = getGoodNumber(element);

        //Lấy sim theo điểm web phong thủy
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\VTIT\\Downloads\\chromedriver-win64\\chromedriver.exe");

        // Khởi tạo trình duyệt Chrome
        WebDriver driver = new ChromeDriver();
        List<SimResult> simData = getSimData(goodNumber, f, driver , listWebScore);
        System.out.println(simData);
        // Chờ cho trang web tải xong
        driver.quit();

        // Add excel data
        exportExcelData(simData);

    }

    private List<SimResult> getSimData(List<SimResult> simResults, int f, WebDriver driver, List<SimResult> listWebScore) {

        if (simResults.isEmpty()) {
            return listWebScore;
        }

        try {
            // Truy cập trang web

            String currentNumber = simResults.get(0).getNumber();
            String stringBuilder = "https://simphongthuy.vn/xem-phong-thuy-sim?" +
                    currentNumber +
                    "&gt=nam&st=0&ns=14-07-1994";
            driver.get(stringBuilder);

            if (f < 1) {
                WebElement date = driver.findElement(By.xpath("//*[@id=\"xem_phong_thuy_sim\"]/div[2]/div[4]/select"));
                Select selectDay = new Select(date);

                selectDay.selectByValue("14");

                WebElement month = driver.findElement(By.xpath("//*[@id=\"xem_phong_thuy_sim\"]/div[2]/div[5]/select"));
                Select selectMonth = new Select(month);

                selectMonth.selectByValue("7");

                WebElement year = driver.findElement(By.xpath("//*[@id=\"xem_phong_thuy_sim\"]/div[2]/div[6]/select"));
                Select selectYear = new Select(year);

                selectYear.selectByValue("1994");
                f++;
            }

            //Tim o nhap so sim
            WebElement inputNumber = driver.findElement(By.id("xptsForm"));
            inputNumber.sendKeys(currentNumber);
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            //Bam nut click
            By submitFormXpt = By.id("submitFormXpt");
            WebElement buttonSubmit = findElementWithRetry(driver, submitFormXpt);

            Actions actions = new Actions(driver);
            actions.moveToElement(buttonSubmit).click().perform();

            buttonSubmit.click();

            // Tìm thẻ span chứa điểm số
            WebElement scoreElement = driver.findElement(By.className("title-block-medium"));

            // Lấy nội dung của thẻ span
            String scoreText = scoreElement.getText();

            String scoreS = scoreText.split(" ")[3];

            // Chuyển đổi nội dung sang số
            Float score = Float.parseFloat(scoreS);

            // Thêm điểm số vào danh sách nếu trên 6 điểm
            if (score > 6) {

                //Add vao list các số với điểm trên 6
                simResults.get(0).setScoreWeb(score.toString());
                listWebScore.add(simResults.get(0));

            }

            simResults.remove(0);
            return getSimData(simResults, f, driver, listWebScore);
        } catch (WebDriverException e) {
            return getSimData(simResults, f, driver, listWebScore);
        }
    }

    private List<String>  getDataViettel() {
        SimRequest simRequest = new SimRequest();
        simRequest.setKey_search("");
        simRequest.setPage(1);
        simRequest.setPage_size(45);
        simRequest.setTotal_record(1);
        simRequest.setIsdn_type(2);
        simRequest.setCaptcha("");
        simRequest.setSid("");
        simRequest.setPage_type("");

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<SimRequest> requestEntity = new HttpEntity<>(simRequest, headers);

        ResponseEntity<SimResponse> responseEntity = restTemplate.exchange(
                "https://vietteltelecom.vn/api/get/sim",
                HttpMethod.POST,
                requestEntity,
                SimResponse.class);

        SimResponse body = responseEntity.getBody();
        assert body != null;
        return body.getData().stream()
                .map(SimData::getIsdn)
                .map("0"::concat)
                .collect(Collectors.toList());

    }

    private static WebElement findElementWithRetry(WebDriver driver, By locator) {
        final int maxAttempts = 3;
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.of(5, ChronoUnit.SECONDS));
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                return element;
            } catch (StaleElementReferenceException e) {
                attempts++;
                if (attempts == maxAttempts) {
                    throw e;
                }
            }
        }
        throw new StaleElementReferenceException("cc");
    }

    public static List<SimResult> getElement(List<String> numbers, List<Integer> life) {
        List<SimResult> listNumberElement = new ArrayList<>();
        numbers.forEach(s -> {
            int i = sumDigits(s);
            if (life.contains(i)) {
                SimResult simResult = new SimResult();
                simResult.setNumber(s);
                listNumberElement.add(simResult);
            }
        });

        return listNumberElement;
    }

    public static List<SimResult> getGoodNumber(List<SimResult> simResults) {
        List<SimResult> simResultGoodNumbers = new ArrayList<>();

        simResults.forEach(s -> {
            SimResult goodSim = goodSim(s.getNumber());
            if (Objects.nonNull(goodSim.getResult())) {
                simResultGoodNumbers.add(goodSim);
            }
        });
        return simResultGoodNumbers;
    }

    public static int sumDigits(String number) {
        Integer sum = 0;
        for (char c : number.toCharArray()) {
            sum += c - '0';
        }
        while (sum > 9) {
            sum = sumDigits(sum.toString());
        }
        return sum;
    }

    public static SimResult goodSim(String number) {
        //Lay 4 so cuoi
        String substring = number.substring(number.length() - 4);
        Double v = Double.parseDouble(substring);

        double f = (v / 80);
        double floor = Math.floor(f);
        int a = (int) ((f - floor) * 80);
        SimResult simResult = mathExcel(a);
        if (Objects.nonNull(simResult.getResult())) {
            simResult.setNumber(number);
        }
        return simResult;
    }


    @SneakyThrows
    public static SimResult mathExcel(Integer simPoint) {
        SimResult simResult = new SimResult();
        String simP = simPoint.toString();
        FileInputStream file = new FileInputStream("D:\\point.xlsx");
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        int rowNum = -1;
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            if (Objects.equals(simP, sheet.getRow(i).getCell(0).getStringCellValue())) {
                rowNum = i;
                break;
            }
        }

        String result;

        String content;

        if (rowNum != 0) {
            result = sheet.getRow(rowNum).getCell(1).getStringCellValue();

            content = sheet.getRow(rowNum).getCell(2).getStringCellValue();

            if (result.trim().equals(Result.GOOD.getName()) || result.trim().equals(Result.VERY_GOOD.getName())) {
                simResult.setResult(result);
                simResult.setContent(content);
            }
        }

        return simResult;
    }

    @SneakyThrows
    private static Workbook createInitExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sim Result");
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 28000);

        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCellNumber = header.createCell(0);
        headerCellNumber.setCellValue("Số điện thoại");
        headerCellNumber.setCellStyle(headerStyle);

        Cell headerCellNumberScore = header.createCell(1);
        headerCellNumberScore.setCellValue("Điểm số");
        headerCellNumberScore.setCellStyle(headerStyle);

        Cell headerCellResult = header.createCell(2);
        headerCellResult.setCellValue("Kết quả");
        headerCellResult.setCellStyle(headerStyle);

        Cell headerCellContent = header.createCell(3);
        headerCellContent.setCellValue("Nội dung");
        headerCellContent.setCellStyle(headerStyle);

        return workbook;
    }

    @SneakyThrows
    private static void exportExcelData(List<SimResult> simResults) {
        try (Workbook workbook = createInitExcel()) {
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setWrapText(true);
            Sheet sheet = workbook.getSheet("Sim Result");
            for (int i = 0; i < simResults.size(); i++) {
                Row row = sheet.createRow(i + 1);

                Cell cellNumber = row.createCell(0);
                cellNumber.setCellValue(simResults.get(i).getNumber());
                cellNumber.setCellStyle(cellStyle);

                Cell cellScore = row.createCell(1);
                cellScore.setCellValue(simResults.get(i).getScoreWeb());
                cellScore.setCellStyle(cellStyle);

                Cell cellResult = row.createCell(2);
                cellResult.setCellValue(simResults.get(i).getResult());
                cellResult.setCellStyle(cellStyle);

                Cell cellContent = row.createCell(3);
                cellContent.setCellValue(simResults.get(i).getContent());
                cellContent.setCellStyle(cellStyle);
            }

            Date date = new Date();
            long time = date.getTime();
            String fileLocation = "D:\\data\\" + "temp" + time + ".xlsx";

            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
        }
    }

    public static void main(String[] args) {
        String a = "1234567890";
        String b = "1234560000";
//        String s = goodSim(b);
//        System.out.println(s);
//        exportExcelData();
    }

}
