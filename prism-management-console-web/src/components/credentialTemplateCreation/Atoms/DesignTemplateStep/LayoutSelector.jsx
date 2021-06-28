import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Col, Radio } from 'antd';
import TemplateLayout0 from '../../../../images/TemplateLayout_0.svg';
import TemplateLayout1 from '../../../../images/TemplateLayout_1.svg';
import TemplateLayout2 from '../../../../images/TemplateLayout_2.svg';
import TemplateLayout3 from '../../../../images/TemplateLayout_3.svg';
import TemplateLayout4 from '../../../../images/TemplateLayout_4.svg';
import './_style.scss';

const layouts = [
  {
    thumb: TemplateLayout0
  },
  {
    thumb: TemplateLayout1
  },
  {
    thumb: TemplateLayout2
  },
  {
    thumb: TemplateLayout3
  },
  {
    thumb: TemplateLayout4
  }
];

const LayoutSelector = () => {
  const { t } = useTranslation();
  const [selected, setSelected] = useState(0);

  const onLayoutChange = ev => {
    setSelected(ev.target.value);
  };

  return (
    <Form.Item
      name="templateLayout"
      label={t('credentialTemplateCreation.step2.style.layout')}
      rules={[
        {
          required: true
        }
      ]}
    >
      <Radio.Group onChange={onLayoutChange}>
        {layouts.map((l, idx) => (
          <Radio value={idx}>
            <Col className={`LayoutOption shadow ${selected === idx ? 'selected' : ''}`}>
              <img className="img-logo" src={l.thumb} alt={`LayouutTemplate_${idx}`} />
            </Col>
          </Radio>
        ))}
      </Radio.Group>
    </Form.Item>
  );
};

LayoutSelector.propTypes = {};

export default LayoutSelector;
