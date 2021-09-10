import * as React from 'react';
import { Collapse } from 'antd';
import { CaretRightOutlined } from '@ant-design/icons';
import './_style.scss';

const Collapsable = () => {

  const { Panel } = Collapse;

function callback(key) {
  console.log(key);
}

const text = `
If you are interested in joining the Atala PRISM Pioneers Program, please complete the registration form. You will receive a short application form, and we will be in touch when the commencement dates for the course are confirmed.
`;

  return (
    <Collapse className="Collapsable" expandIconPosition={'right'} defaultActiveKey={['1']} onChange={callback}>
    <Panel header="How can I register for the Program?" key="1">
      <p>{text}</p>
    </Panel>
    <Panel header="This is panel header 2" key="2">
      <p>{text}</p>
    </Panel>
    <Panel header="This is panel header 3" key="3">
      <p>{text}</p>
    </Panel>
  </Collapse>
  );
};

export default Collapsable;

