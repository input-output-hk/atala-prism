import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Col, Radio } from 'antd';
import TemplateLayout0 from '../../../../images/TemplateLayout_0.svg';
import TemplateLayout1 from '../../../../images/TemplateLayout_1.svg';
import TemplateLayout2 from '../../../../images/TemplateLayout_2.svg';
import TemplateLayout3 from '../../../../images/TemplateLayout_3.svg';
import TemplateLayout4 from '../../../../images/TemplateLayout_4.svg';
import './_style.scss';
import { useTemplateContext } from '../../../providers/TemplateContext';

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
  const { templateSettings } = useTemplateContext();

  return (
    <Form.Item
      name="layout"
      label={t('credentialTemplateCreation.step2.style.layout')}
      rules={[
        {
          required: true
        }
      ]}
    >
      <Radio.Group>
        {layouts.map((l, idx) => (
          <Radio value={idx}>
            <Col
              className={`LayoutOption shadow ${
                templateSettings.templateLayout === idx ? 'selected' : ''
              }`}
            >
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
