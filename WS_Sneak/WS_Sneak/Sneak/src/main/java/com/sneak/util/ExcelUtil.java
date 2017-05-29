package com.sneak.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import com.sneak.dao.QuoteDAO;
import com.sneak.modelo.Estadistica;
import com.sneak.modelo.Quote;
import com.sneak.services.QuoteServiceImpl;

import oracle.jdbc.pool.OracleDataSource;

public class ExcelUtil {

	@Autowired
	private QuoteServiceImpl qService;

	// @Bean
	// public static void getqService (QuoteService a) {
	// qService = a;
	// }
	private static Connection conex;

	/**
	 * Checkea si el excel ya ha sido cargado
	 * 
	 * @param pathExcel
	 * @return boolean
	 */
	public static boolean checkXLS(String pathExcel) {
		boolean ret = false;

		String nombre = pathExcel.substring(pathExcel.lastIndexOf("\\") + 1);

		Connection conex = null;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

			conex = DriverManager.getConnection(url, "root", "");
			ResultSet rs = conex.createStatement()
					.executeQuery("SELECT * FROM snk_rep_carga WHERE fichero='" + nombre + "'");

			if (rs.next()) {
				ret = false;
			} else {
				ret = true;
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conex != null && !conex.isClosed()) {
					conex.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}

	public static int getNextId() { // Preguntar
		Quote q = new Quote();

		String tabla = "snk_rep_carga";
		int ret = -1;
		String bbdd = "sneak";
		String query = "SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + bbdd
				+ "' AND TABLE_NAME ='" + tabla + "'";
		try {
			String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

			conex = DriverManager.getConnection(url, "root", "");
			ResultSet rs = conex.createStatement().executeQuery(query);

			if (rs.next()) {
				ret = rs.getInt(1);
			} else {
				q.setMsg("No se encuentra el autoincremental para la tabla " + tabla + ".");
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conex != null && !conex.isClosed()) {
					conex.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}

	public boolean updtUltimaCarga(int cargaid) {
		boolean ret = false;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
			conex = DriverManager.getConnection(url, "root", "");

			String upd = "UPDATE snk_conf SET valor= " + cargaid + " WHERE clave='ultima_carga'";
			Statement stmt = conex.createStatement();
			int chkUpdt = stmt.executeUpdate(upd);

			if (chkUpdt == 0) {
				String insert = "INSERT INTO snk_conf (clave,valor) values ('ultima_carga'," + cargaid + ")";
				stmt = conex.createStatement();
				stmt.executeUpdate(insert);
				ret = true;
			} else {
				ret = true;
			}

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conex != null && !conex.isClosed()) {
					conex.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * Carga un excel en BBDD dada una ruta espec�fica
	 * 
	 * @param pathExcel
	 */
	public Quote cargaExcel(String pathExcel) {
		Quote quote = new Quote();

		conex = null;

		if (!ExcelUtil.checkXLS(pathExcel)) {
			try {
				String nombre = pathExcel.substring(pathExcel.lastIndexOf("\\") + 1);

				Class.forName("com.mysql.cj.jdbc.Driver");
				String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
				conex = DriverManager.getConnection(url, "root", "");

				String query = "SELECT id FROM snk_rep_carga WHERE fichero= '" + nombre + "'";
				Statement stmt = conex.createStatement();
				ResultSet rs = stmt.executeQuery(query);

				if (rs.next()) {
					int cargaid = rs.getInt(1);
					updtUltimaCarga(cargaid);
					quote.setMsg(
							"�Ese fichero ya ha sido cargado!. Se utiliza ese fichero para obtener las estad�sticas.");
				} else {
					quote.setMsg("Se ha producido un error al cargar el fichero.");
				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				FileInputStream file = new FileInputStream(new File(pathExcel));

				XSSFWorkbook workbook = new XSSFWorkbook(file);
				XSSFSheet sheet = workbook.getSheetAt(0);

				Iterator<Row> rowIterator = sheet.iterator();
				int cargaId = getNextId();
				String nombre = pathExcel.substring(pathExcel.lastIndexOf("\\") + 1);
				Timestamp fecha = new Timestamp(new Date().getTime());
				int total = sheet.getLastRowNum();

				Class.forName("com.mysql.cj.jdbc.Driver");

				String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

				conex = DriverManager.getConnection(url, "root", "");
				PreparedStatement ps = conex
						.prepareStatement("INSERT INTO snk_rep_carga (fecha,fichero,total) values (?,?,?)");
				ps.setTimestamp(1, fecha);
				ps.setString(2, nombre);
				ps.setInt(3, total);
				int chkInsert = ps.executeUpdate();

				if (chkInsert == 0) {
					quote.setMsg("Hubo alg�n error intentando insertar la cabecera.");
				} else {
					qService = new QuoteServiceImpl(new QuoteDAO(conex));
					Row row;

					rowIterator.next();
					updtUltimaCarga(cargaId);
					while (rowIterator.hasNext()) {
						row = rowIterator.next();

						Quote q = new Quote(row.getCell(1).getStringCellValue());
						q.setCargaId(cargaId);

						q.setCreated(new Timestamp(row.getCell(0).getDateCellValue().getTime()));

						String statCd = row.getCell(2, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (statCd == null || statCd.isEmpty()) {
							statCd = "";
						}
						q.setStatCd(statCd);

						String subestado = row.getCell(3, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (subestado == null || subestado.isEmpty()) {
							subestado = "";
						}
						q.setxVfStatus(subestado);

						String tipo = row.getCell(4, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (tipo == null || tipo.isEmpty()) {
							tipo = "";
						}
						q.setQuoteType(tipo);

						String login = row.getCell(5, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (login == null || login.isEmpty()) {
							login = "";
						}
						q.setLogin(login);

						String orderref = row.getCell(6, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (orderref == null || orderref.isEmpty()) {
							orderref = "";
						}
						q.setOrderRef(orderref);

						String incidencia = row.getCell(7, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (incidencia == null || incidencia.isEmpty()) {
							incidencia = "";
						}
						q.setIncidencia(incidencia);

						String responsable = row.getCell(8, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (responsable == null || responsable.isEmpty()) {
							responsable = "";
						}
						q.setResponsable(responsable);

						String responsableNuevo = row.getCell(9, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (responsableNuevo == null || responsableNuevo.isEmpty()) {
							responsableNuevo = "";
						}
						q.setResponsableNuevo(responsableNuevo);

						String estado = row.getCell(10, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (estado == null || estado.isEmpty()) {
							estado = "";
						}
						q.setEstado(estado);

						String codigo = row.getCell(11, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (codigo == null || codigo.isEmpty()) {
							codigo = "";
						}
						q.setCodigo(codigo);

						String descripcion = row.getCell(12, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (descripcion == null || descripcion.isEmpty()) {
							descripcion = "";
						}
						q.setDescripcion(descripcion);

						String planItemId = row.getCell(13, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (planItemId == null || planItemId.isEmpty()) {
							planItemId = "";
						}
						q.setPlanItemId(planItemId);

						String nativeErrorDesc = row.getCell(14, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
						if (nativeErrorDesc == null || nativeErrorDesc.isEmpty()) {
							nativeErrorDesc = "";
						}
						q.setNativeErrorDesc(nativeErrorDesc);

						qService.insertToDB(q);
						quote.setMsg("La carga se ha realizado correctamente.");

					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (conex != null && !conex.isClosed()) {
						conex.close();
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return quote;
	}

	public String updtDatosResponsable(String responsable) {
		String msg = "";
		Connection conn = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");

			String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
			conex = DriverManager.getConnection(url, "root", "");

			String sql1 = "SELECT QUOTEREF FROM SNK_REP_DATO WHERE CARGAID = (SELECT VALOR FROM SNK_CONF WHERE CLAVE = 'ultima_carga') AND RESPONSABLE_NUEVO = '"
					+ responsable
					+ "' AND ESTADO_ACTUAL NOT IN ('Completado','Cancelado','Parcialmente Completado', 'Rechazado')";

			Statement stmt = conex.createStatement();
			ResultSet rs = stmt.executeQuery(sql1);

			if (rs.first()) {
				// String quotes = genWhereClause(rs, "QUOTEREF");
				ArrayList<String> lQuotes = genListQuotes(rs, "QUOTEREF");
				String quotes = genWhereClause(lQuotes);

				String sql2 = "SELECT QUOTEREF, Q_STAT_CD, INCIDENT FROM DASHBOARD WHERE QUOTEREF IN (" + quotes + ")";

				Class.forName("oracle.jdbc.OracleDriver");
				// System.setProperty("oracle.net.tns_admin",
				// "C:\\app\\6000848\\product\\11.2.0\\client_1\\network\\admin");
				OracleDataSource ods = new OracleDataSource();
				ods.setLoginTimeout(5000);
				String IALUser = "VF_ES_IAL";
				String IALPass = "VF_ES_IAL98";
				String UrlIAL = "jdbc:oracle:oci:@(DESCRIPTION = (ADDRESS = (PROTOCOL = TCP) (HOST = 62.87.14.58) (PORT = 1521)) (CONNECT_DATA = (SERVER = dedicated) (SERVICE_NAME = IALAFFP_SRV)))";
				ods.setURL(UrlIAL);
				ods.setUser(IALUser);
				ods.setPassword(IALPass);
				conn = ods.getConnection();

				Statement stmt2 = conn.createStatement();
				ResultSet rs2 = stmt2.executeQuery(sql2);

				String sql3 = "UPDATE snk_rep_dato SET ESTADO_ACTUAL = ?, INCIDENT_ACTUAL = ? WHERE QUOTEREF = ?";
				PreparedStatement pstmt = conex.prepareStatement(sql3);
				while (rs2.next()) {
					String estado = rs2.getString("Q_STAT_CD");
					String incident = rs2.getString("INCIDENT");
					String quoteref = rs2.getString("QUOTEREF");

					pstmt.setString(1, estado);
					pstmt.setString(2, incident);
					pstmt.setString(3, quoteref);
					pstmt.executeUpdate();
					lQuotes.remove(quoteref);
				}

				Iterator<String> ite = lQuotes.iterator();
				while (ite.hasNext()) {
					String quoteref = ite.next();
					pstmt.setString(1, "Completado");
					pstmt.setString(2, "");
					pstmt.setString(3, quoteref);
					pstmt.executeUpdate();
				}
			} else {
				msg = "�No se puede actualizar sin antes cargar un reparto!.";
				return msg;
			}

			// System.out.println(quotes);
			// System.out.println("-------------------------------------------------------------------");
			// System.out.println(lQuotes.toString());

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		msg = "OK";
		return msg;
	}

	private String genWhereClause(ArrayList<String> lQuotes) {
		String ret = "";
		Iterator<String> ite = lQuotes.iterator();
		while (ite.hasNext()) {
			ret += "'" + ite.next() + "',";
		}
		ret = ret.substring(0, ret.length() - 1);
		return ret;
	}

	private ArrayList<String> genListQuotes(ResultSet rs, String nombreCol) {
		ArrayList<String> ret = new ArrayList<>();

		try {
			while (rs.next()) {
				ret.add(rs.getString(nombreCol));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public String genWhereClause(ResultSet rs, String nombreCol) {
		String ret = "";

		try {
			while (rs.next()) {
				ret += "'" + rs.getString(nombreCol) + "',";
			}
			ret = ret.substring(0, ret.length() - 1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public Estadistica generaEstadisticas(String responsable) {
		Estadistica ret = new Estadistica();
		ret.setResponsable(responsable);
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/sneak?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
			conex = DriverManager.getConnection(url, "root", "");
			String sql1 = "SELECT QUOTEREF,STAT_CD, INCIDENCIA, ESTADO_ACTUAL,INCIDENT_ACTUAL FROM SNK_REP_DATO WHERE CARGAID = (SELECT VALOR FROM SNK_CONF WHERE CLAVE = 'ultima_carga') AND RESPONSABLE_NUEVO = '"
					+ responsable + "'";
			Statement stmt = conex.createStatement();
			ResultSet rs = stmt.executeQuery(sql1);

			while (rs.next()) {
				String quoteref = rs.getString("QUOTEREF");
				String est_old = rs.getString("STAT_CD");
				String cat_old = rs.getString("INCIDENCIA");
				String est_new = rs.getString("ESTADO_ACTUAL");
				String cat_new = rs.getString("INCIDENT_ACTUAL");

				ret.suma_total();
				// Comprobamos si es blanca o catalogada.
				if (cat_old == null || cat_old.isEmpty() || cat_old.equals("-")) {
					// blanca
					ret.suma_blank_total();
					if (est_new.equals("Completado") || est_new.equals("Cancelado")
							|| est_new.equals("Parcialmente Completado") || est_new.equals("Rechazado")) {
						ret.suma_blank_completas();
					} else {
						if (cat_old.equals(cat_new)) {
							ret.suma_blank_sin_tocar();
						} else {
							ret.suma_blank_catalogadas();
						}
					}
				} else {
					// catalogada
					ret.suma_catalog_total();
					// Estados finales: 'Completado','Cancelado','Parcialmente
					// Completado', 'Rechazado'
					if (est_new.equals("Completado") || est_new.equals("Cancelado")
							|| est_new.equals("Parcialmente Completado") || est_new.equals("Rechazado")) {
						ret.suma_catalog_completas();
					} else {
						if (cat_old.equals(cat_new)) {
							ret.suma_catalog_sin_tocar();
						} else {
							ret.suma_catalog_recatalogada();
						}
					}
				}
			}

			// Generamos porcentajes e incluimos en los string de Estadistica:
			// Blancas:
			// Completas

			String str_blank_completas = ret.getBlank_completas() + "/" + ret.getBlank_total() + "  ("
					+ genPorcent(ret.getBlank_completas(), ret.getBlank_total()) + "%)";
			ret.setStr_blank_completa(str_blank_completas);

			// Catalogadas
			String str_blank_catalogadas = ret.getBlank_catalogadas() + "/" + ret.getBlank_total() + "  ("
					+ genPorcent(ret.getBlank_catalogadas(), ret.getBlank_total()) + "%)";
			ret.setStr_blank_catalogada(str_blank_catalogadas);

			// Sin tocar
			String str_blank_sin_tocar = ret.getBlank_sin_tocar() + "/" + ret.getBlank_total() + "  ("
					+ genPorcent(ret.getBlank_sin_tocar(), ret.getBlank_total()) + "%)";
			ret.setStr_blank_sin_tocar(str_blank_sin_tocar);

			// Catalogadas
			// Completas
			String str_catalog_completas = ret.getCatalog_completas() + "/" + ret.getCatalog_total() + "  ("
					+ genPorcent(ret.getCatalog_completas(), ret.getCatalog_total()) + "%)";
			ret.setStr_catalog_completa(str_catalog_completas);

			// Recatalogadas
			String str_catalog_recatalog = ret.getCatalog_recatalogada() + "/" + ret.getCatalog_total() + "  ("
					+ genPorcent(ret.getCatalog_recatalogada(), ret.getCatalog_total()) + "%)";
			ret.setStr_catalog_recatalogada(str_catalog_recatalog);

			// Sin tocar
			String str_catalog_sin_tocar = ret.getCatalog_sin_tocar() + "/" + ret.getCatalog_total() + "  ("
					+ genPorcent(ret.getCatalog_sin_tocar(), ret.getCatalog_total()) + "%)";
			ret.setStr_catalog_sin_tocar(str_catalog_sin_tocar);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * M�todo para generar el porcentaje de un n�mero dado el total.
	 * 
	 * @param num
	 * @param total
	 * @return
	 */
	private double genPorcent(int num, int total) {
		double ret;
		ret = (double) (num * 100) / total;
		ret = redondearDecimales(ret, 2);
		return ret;
	}

	public static double redondearDecimales(double valorInicial, int numeroDecimales) {
		double parteEntera, resultado;
		resultado = valorInicial;
		parteEntera = Math.floor(resultado);
		resultado = (resultado - parteEntera) * Math.pow(10, numeroDecimales);
		resultado = Math.round(resultado);
		resultado = (resultado / Math.pow(10, numeroDecimales)) + parteEntera;
		return resultado;
	}

}
