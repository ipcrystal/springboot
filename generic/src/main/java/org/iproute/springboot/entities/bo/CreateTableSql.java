package org.iproute.springboot.entities.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateTableSql
 *
 * @author winterfell
 * @since 2022/1/23
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateTableSql {

    private String table;

    private String createTable;
}