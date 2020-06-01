import React from 'react';
import { Collapse, Icon } from 'antd';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const panelsData = [
  {
    key: '1',
    name: 'credentials',
    bulletsNumber: 5
  },
  {
    key: '2',
    name: 'wallet',
    bulletsNumber: 4
  },
  {
    key: '3',
    name: 'crypto',
    bulletsNumber: 5
  }
];

const ItemCollapse = () => {
  const { t } = useTranslation();

  const { Panel } = Collapse;

  const customPanelStyle = {
    borderRadius: 4,
    marginBottom: 24,
    border: 0,
    overflow: 'hidden',
    width: '27.5%'
  };

  const bulletsRender = (name, bulletsNumber) => {
    let bullets = [];
    for (let i = 1; i <= bulletsNumber; i++) {
      bullets.push(
        <li>
          <p>{t(`landing.intro.itemIcon.${name}.bullet${i}`)}</p>
        </li>
      );
    }
    return bullets;
  };

  const panels = panelsData.map(panel => (
    <Panel header={t('landing.intro.itemIcon.readMore')} key={panel.key} style={customPanelStyle}>
      <div className="BulletItems">
        <ul>{bulletsRender(panel.name, panel.bulletsNumber)}</ul>
      </div>
    </Panel>
  ));

  return (
    <div className="CollapseContainer">
      <Collapse
        className="ItemDetail"
        bordered={false}
        expandIcon={({ isActive }) => <Icon type="caret-right" rotate={isActive ? 90 : 0} />}
      >
        {panels}
      </Collapse>
    </div>
  );
};

export default ItemCollapse;
