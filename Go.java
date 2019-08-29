package servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tools.DBTools;
import tools.DateMinus;
import java.sql.ResultSet;
@WebServlet("/Go")
public class Go extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public Go() {
        super();
    }
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		//1���ܳ����볡go.jsp�������Ĳ�����logID	logNewLicenseNo	logNewEndTime��
		//String lid = request.getParameter("logOldId");
		String licenseno = request.getParameter("logNewLicenseNo");
		String endtime = request.getParameter("logNewEndTime");
		String timedhms = request.getParameter("logTimeDHMS");
		String ischarge = request.getParameter("logIsCharge");
		System.out.println("timedhms:"+timedhms);
		System.out.println("ischarge:"+ischarge);
		//2���ݳ��ƺ�logNewLicenseNo�ڳ�λʹ�ü�¼log���ҳ�λ��cp_number
		//2.1ƴдSQL���
		String sql = "SELECT log_id,log_licenseno,cp_number,log_starttime FROM LOG WHERE log_licenseno = '"+licenseno+"' AND log_endtime IS NULL AND log_flag = 0";
		System.out.println("sql:" + sql);
		//�ǿ�У��
		if(licenseno == null || licenseno.length()==0 || licenseno.equals("all") ||
				endtime == null || endtime.length()==0){
			response.setContentType("text/html; charset=UTF-8");
			PrintWriter pw = response.getWriter();
			pw.write("<script>alert('���ݲ���Ϊ��');window.location.href='go.jsp'</script>");
			pw.flush();
			pw.close();
		}else{
			//2.2����JDBC
			DBTools db_query = DBTools.getDb();
			//2.3ִ��SQL���
	       	ResultSet rs = db_query.query(sql);
	       	try {
				while(rs.next()){
					//���ܲ�ѯ��䴫�����ĳ�λ��
					String lid = rs.getString("log_id");
					String pno = rs.getString("cp_number");
					String lst = rs.getString("log_starttime");
					//3���ݳ�λ��cp_number�ڳ�λ��port�и��³�λ״̬Ϊδռ��
					//3.1ƴдSQL���
					String go_sql = "Update port set port_status = 'δռ��' where port_no = '"+pno+"' AND port_flag = 0";
					System.out.println("go_sql :" + go_sql);
					//3.2����JDBC
					DBTools db = DBTools.getDb();
					//3.3ִ��SQL���
					int row = db.update(go_sql);
					if (row > 0){
						System.out.println("update portstatus ok");
					}else{
						System.out.println("update portstatus err");
					}
					//����ʱ�䣨�룩
					DateMinus time = new DateMinus();
					long miaos = time.diffMiao(endtime, lst);
					//�Ʒ�
					long hours = time.diffHours(endtime, lst);
					System.out.println(hours);
					String toll_sql = "SELECT toll_id,toll_time1,toll_fee1,toll_fee2 FROM toll where toll_id = 1";
					System.out.println("toll_sql:"+toll_sql);
					DBTools toll_db = DBTools.getDb();
					ResultSet toll_rs = toll_db.query(toll_sql);
					int tt1 = 0;
					int tf1 = 0;
					/*int tt2 = 0;*/
					int tf2 = 0;
					while(toll_rs.next()){
						tt1 = toll_rs.getInt("toll_time1");
						tf1 = toll_rs.getInt("toll_fee1");
						//tt2 = toll_rs.getInt("toll_time2");
						tf2 = toll_rs.getInt("toll_fee2");
					}
					float lmy1;
					if(hours >= 0 && hours < tt1){
						lmy1 = tf1;
					}else{
						lmy1 = tf1 + (hours - tt1) * tf2 + 1;
					}
					//�����Ƿ�Ϊ�꿨�ͻ��Ƿ����
					String vip_sql = "SELECT vip_licenseno ,vip_starttime,vip_endtime,vip_discountyear FROM vip WHERE vip_licenseno = '"+licenseno+"'AND vip_flag = 0 ";
					System.out.println("vip_sql:"+vip_sql);
					DBTools vip_db = DBTools.getDb();
					ResultSet vip_rs = vip_db.query(vip_sql);
					while(vip_rs.next()){
						String vst = vip_rs.getString("vip_starttime");
						String vet = vip_rs.getString("vip_endtime");
						String vdy = vip_rs.getString("vip_discountyear");
						//��ѯ�ۿ�
						String discount_sql = "SELECT discount_num1,discount_num2 FROM discount WHERE discount_id = 1";
						System.out.println("discount_sql:"+discount_sql);
						DBTools discount_db = DBTools.getDb();
						ResultSet discount_rs = discount_db.query(discount_sql);
						float dc1 = 0;
						float dc2 = 0;
						while(discount_rs.next()){
							dc1 = discount_rs.getFloat("discount_num1");
							dc2 = discount_rs.getFloat("discount_num2");
						}
						if(endtime.compareTo(vet) < 0 && endtime.compareTo(vst) > 0){
							if(Integer.parseInt(vdy) == 1){
								lmy1 = lmy1 * dc1;
							}else{
								lmy1 = lmy1 * dc2;
							}
						}
					}
					//4���ݽ��ܵ���ʱ������볡ʱ��Ϊ��ǰʱ��,�ѷ��ø��µ�log����
					//��ʱ����µ�log����
					//��ͨ�����Ʒѣ����ischargeֵ���������log
					//��������ֳ�������Ʒѣ����nullֵ��0����log
					if(ischarge == null){
						lmy1 = 0;
					}
					//4.1ƴдSQL���
					String endtime_sql = "Update log set log_endtime = '"+endtime+"' ,log_times = '"+miaos+"', log_money = '"+lmy1+"' ,log_timedhms = '"+timedhms+"' where log_id = '"+lid+"' AND log_flag = 0";
					System.out.println("endtime_sql :" + endtime_sql);
					//4.2����JDBC
					DBTools db_endtime = DBTools.getDb();
					//4.3ִ��SQL���
					int row_endtime = db_endtime.update(endtime_sql);
					if (row_endtime > 0){
						System.out.println("update endtime ok");
						//5������ת����ӡ�վ�ҳ��,��ҪlogID�����ƺţ���λ�ţ��볡ʱ�䣬�볡ʱ��
						//5.1ƴдSQL����ȡ�볡ʱ��
						String sql_query = "SELECT log_starttime FROM LOG WHERE log_endtime = '"+endtime+"' AND log_flag = 0";
						System.out.println("sql_query:" + sql_query);
						//2.2����JDBC
						DBTools db_billQuery = DBTools.getDb();
						//2.3ִ��SQL���
				       	ResultSet rs_query = db_billQuery.query(sql_query);
				       	while(rs_query.next()){
				       		String llsn = licenseno;
				       		String cpn = pno;
				       		String let = endtime;
				       		String lmy = String.valueOf(lmy1);
				       		String tdhms = timedhms;
				       		System.out.println(lid);
							System.out.println(llsn);
							System.out.println(cpn);
							System.out.println(lst);
							System.out.println(let);
							System.out.println(lmy);
							System.out.println(tdhms);
							//��logID�����ƺţ���λ�ţ��볡ʱ�䣬�볡ʱ�䣬�ƷѴ�servlet��ֵ��jsp
				       		request.setAttribute("Valuelid", lid);
				       		request.setAttribute("Valuellsn", llsn);
				       		request.setAttribute("Valuecpn", cpn);
				       		request.setAttribute("Valuelst", lst);
				       		request.setAttribute("Valuelet", let);
				       		request.setAttribute("Valuelmy", lmy);
				       		request.setAttribute("Valuetdhms", tdhms);
				       		request.getRequestDispatcher("print.jsp").forward(request, response);
				       	}
						//response.sendRedirect("print.jsp");
					}else{
						//���򵯴�����
						System.out.println("update endtime err");
						response.setContentType("text/html; charset=UTF-8");
						PrintWriter pw = response.getWriter();
						pw.write("<script>alert('�볡ʧ�ܣ������ԭ��');window.location.href='go.jsp'</script>");
						pw.flush();
						pw.close();
						response.sendRedirect("log.jsp");
					}
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}