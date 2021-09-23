import React from 'react';
import { Collapse, Icon } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const bulletsNumber = 5;

const ItemCollapse = ({ name }) => {
  const { t } = useTranslation();

  const { Panel } = Collapse;

  const customPanelStyle = {
    borderRadius: 4,
    marginBottom: 24,
    border: 0,
    overflow: 'hidden'
  };

  const bulletsRender = () => {
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

  return (
    <div className="CollapseContainer">
      <Collapse
        className="ItemDetail"
        bordered={false}
        expandIcon={({ isActive }) => <Icon type="caret-right" rotate={isActive ? 90 : 0} />}
      >
        <Panel header={t('landing.intro.itemIcon.readMore')} key={name} style={customPanelStyle}>
          <div className="BulletItems">
            <ul>{bulletsRender()}</ul>
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default ItemCollapse;
