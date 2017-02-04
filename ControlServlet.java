package control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hibernate.dao.PricewebDAO;
import hibernate.dao.entity.MTGSETIF;
import hibernate.dao.entity.PRICEWEB;
import hibernate.dao.impl.mtgsetifDAOImpl;
import hibernate.dao.impl.pricewebDAOImpl;
import query.QueryByPk;
import utils.ExchangeRate;
import utils.ProcessDataSet;

public class ControlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	


	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 避免Dispatcher Forward 的頁面出現亂碼
		response.setContentType("text/html;charset=UTF-8");
		String action_id=(String)request.getParameter("Action_ID");

		
		//新增系列
		if(action_id.equals("NEW_SET")){
			doNewSet(request, response);
		}
		
		//更新卡價
		if(action_id.equals("UPDATE_PRICE")){
			doOneClickUpdate(request, response);
		}
	}

	private void doNewSet(HttpServletRequest request, HttpServletResponse response) {
		// 新增系列資訊
		String setcode = request.getParameter("SETCODE"); 
		System.out.println(setcode);
		
		//取得bean
		MTGSETIF mtgsetif = new MTGSETIF();

		QueryByPk qpk = new QueryByPk(); 
		ProcessDataSet pds = qpk.findAllMTGSETIF();
		pds.start();
		while (pds.next()) {
			String setCode = pds.getString("SETCODE");
		}

		mtgsetif.setSETCODE("A");
		mtgsetif.setSETNAME_EN("Alpha");
		mtgsetifDAOImpl mImpl = new mtgsetifDAOImpl();
		//mImpl.add(mtgsetif);
		//mImpl.delete(mtgsetif);
		
	}

	private void doOneClickUpdate(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 一鍵抓取國外網站卡價
		// 匯率手動輸入

		QueryByPk qpk = new QueryByPk();
		ProcessDataSet pds = qpk.findAllPRICEWEB();
		pds.start();
		HashMap<String, ArrayList<String>> cardMap = new HashMap<String, ArrayList<String>>();
		while (pds.next()) {

			ExchangeRate exchange = new ExchangeRate();
			// String generate_URL =
			// "https://www.mtggoldfish.com/index/KLD_F#paper";
			String generate_URL = pds.getString("WEB_URL");
			String inputLine = "";
			String cardName = "";
			String setName = "";
			String cardPrice_US = "";
			String cardPrice_TW = "";
			//HashMap<String, ArrayList<String>> cardMap = new HashMap<String, ArrayList<String>>();

			boolean foilFlag = Pattern.matches(".*F#paper", generate_URL); // 判斷是否為閃卡頁面

			Pattern cardPattern = Pattern.compile("\">(.*)</a>"); // 取得牌名Regex
			Pattern setPattern = Pattern.compile(">(.*)<"); // 取得系列Regex
			Matcher matcher = null;
			try {
				// 獲得網頁URL
				URL data = new URL(generate_URL);
				HttpURLConnection con = (HttpURLConnection) data.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while ((inputLine = in.readLine()) != null) {
					// 解析每行有#paper的資料
					if (Pattern.matches(".*#paper.*", inputLine)) {
						matcher = cardPattern.matcher(inputLine);
						if (matcher.find()) {
							// 取得牌名
							cardName = matcher.group(1);
						}
						inputLine = in.readLine();
						matcher = setPattern.matcher(inputLine);
						if (matcher.find()) {
							// 系列名稱
							setName = matcher.group(1);
						}
						// 跳過三行
						inputLine = in.readLine();
						inputLine = in.readLine();
						inputLine = in.readLine();
						// 價格
						cardPrice_US = inputLine; // 先用String 來存
						cardPrice_TW = exchange.convert(cardPrice_US, request.getParameter("exchangeRate"));

						ArrayList<String> cardValue = new ArrayList<String>();
						cardValue.add(setName);
						cardValue.add(cardPrice_TW);
						if (foilFlag) {
							cardValue.add("F");
						} else {
							cardValue.add("N");
						}
						cardMap.put(cardName, cardValue);
					}
				}
				in.close();
				con.disconnect();
				//request.setAttribute("cardNewPrice", cardMap);
				// RequestDispatcher dispatcher =
				// request.getRequestDispatcher("jsp/priceUpdateSuccess.jsp");
				// dispatcher.forward(request, response);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		request.setAttribute("cardNewPrice", cardMap);
		RequestDispatcher dispatcher = request.getRequestDispatcher("jsp/priceUpdateSuccess.jsp");
		dispatcher.forward(request, response);

	}

}
