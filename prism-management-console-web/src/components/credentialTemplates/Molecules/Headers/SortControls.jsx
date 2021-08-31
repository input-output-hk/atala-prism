import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Dropdown, Menu } from 'antd';
import { DownOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import { SORTING_DIRECTIONS, TEMPLATES_SORTING_KEYS } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { templateSortingShape } from '../../../../helpers/propShapes';

const SortControls = ({ sortingBy, setSortingBy, sortDirection, setSortDirection }) => {
  const { t } = useTranslation();

  const sortingOptions = Object.keys(TEMPLATES_SORTING_KEYS);

  const sortingOptionsMenu = (
    <Menu onClick={({ key }) => setSortingBy(key)}>
      {sortingOptions.map(column => (
        <Menu.Item key={column}>{t(`templates.table.columns.${column}`)}</Menu.Item>
      ))}
    </Menu>
  );

  const sortAscending = sortDirection === SORTING_DIRECTIONS.ascending;

  const toggleSorting = () =>
    setSortDirection(sortAscending ? SORTING_DIRECTIONS.descending : SORTING_DIRECTIONS.ascending);

  return (
    <div className="TableOptions">
      <div className="LeftOptions">
        <Button
          className="TableOptionButton no-border"
          onClick={toggleSorting}
          large
          icon={
            sortAscending ? (
              <SortAscendingOutlined style={{ fontSize: '16px' }} />
            ) : (
              <SortDescendingOutlined style={{ fontSize: '16px' }} />
            )
          }
        />
        <Dropdown overlay={sortingOptionsMenu} trigger={['click']}>
          {
            <CustomButton
              buttonText={t(sortingBy ? `templates.table.columns.${sortingBy}` : 'actions.sortBy')}
              buttonProps={{ className: 'theme-link TableOptionButton', icon: <DownOutlined /> }}
            />
          }
        </Dropdown>
      </div>
    </div>
  );
};

SortControls.propTypes = templateSortingShape;

export default SortControls;
