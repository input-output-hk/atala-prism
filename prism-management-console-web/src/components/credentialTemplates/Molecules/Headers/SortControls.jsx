import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { Button, Dropdown, Menu } from 'antd';
import { DownOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import { SORTING_DIRECTIONS, TEMPLATES_SORTING_KEYS } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useTemplateUiState } from '../../../../hooks/useTemplateStore';

const { ascending } = SORTING_DIRECTIONS;

const SortControls = observer(() => {
  const { t } = useTranslation();
  const { sortDirection, toggleSortDirection, sortingBy, setSortingBy } = useTemplateUiState();

  const sortingOptions = Object.keys(TEMPLATES_SORTING_KEYS);

  const sortingOptionsMenu = (
    <Menu onClick={({ key }) => setSortingBy(key)}>
      {sortingOptions.map(column => (
        <Menu.Item key={column}>{t(`templates.table.columns.${column}`)}</Menu.Item>
      ))}
    </Menu>
  );

  const sortAscending = sortDirection === ascending;

  return (
    <div className="TableOptions">
      <div className="LeftOptions">
        <Button
          className="TableOptionButton no-border"
          onClick={toggleSortDirection}
          icon={
            sortAscending ? (
              <SortAscendingOutlined style={{ fontSize: '16px' }} />
            ) : (
              <SortDescendingOutlined style={{ fontSize: '16px' }} />
            )
          }
        />
        <Dropdown overlay={sortingOptionsMenu} trigger={['click']}>
          <CustomButton
            overrideClassName="theme-link TableOptionButton"
            buttonText={t(sortingBy ? `templates.table.columns.${sortingBy}` : 'actions.sortBy')}
            buttonProps={{ icon: <DownOutlined /> }}
          />
        </Dropdown>
      </div>
    </div>
  );
});

export default SortControls;
