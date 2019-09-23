package org.finabile.fineract.portfolio.code.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class FinabileCodeValueReadPlatformServiceImpl implements FinabileCodeValueReadPlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;
	private final FinabileCodeValueMapper finabileCodeValueMapper = new FinabileCodeValueMapper();

	@Autowired
	public FinabileCodeValueReadPlatformServiceImpl(final RoutingDataSource dataSource,
			final PlatformSecurityContext context) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.context = context;
	}

	@Override
	public List<FinabileCodeValueData> getAddressTypes() {

		this.context.authenticatedUser();

		final StringBuilder sqlBuilder = new StringBuilder(200);
		sqlBuilder.append("select ");
		sqlBuilder.append(this.finabileCodeValueMapper.loanSchema());

		return this.jdbcTemplate.query(sqlBuilder.toString(), this.finabileCodeValueMapper, new Object[] {});
	}

	@Override
	public FinabileCodeValueData getAddressTypeValue(final Long valueId) {

		this.context.authenticatedUser();

		final StringBuilder sqlBuilder = new StringBuilder(200);
		sqlBuilder.append("select ");
		sqlBuilder.append(this.finabileCodeValueMapper.loanSchema() + " and cv.id = ? ");

		return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), this.finabileCodeValueMapper,
				new Object[] { valueId });
	}

	private static final class FinabileCodeValueMapper implements RowMapper<FinabileCodeValueData> {
		public String loanSchema() {
			return "  cv.code_value, cv.id from m_code c " + " left join m_code_value cv on cv.code_id = c.id "
					+ " where c.code_name = 'ADDRESS_TYPE'";

		}

		@Override
		public FinabileCodeValueData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final Long codeValueId = rs.getLong("id");
			final String codeValue = rs.getString("code_value");

			return new FinabileCodeValueData(codeValueId, codeValue, null);
		}
	}

}
