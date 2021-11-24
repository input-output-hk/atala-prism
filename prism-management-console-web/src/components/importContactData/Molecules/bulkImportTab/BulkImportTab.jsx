import React from 'react';
import { Select, Tag, Checkbox } from 'antd';
import { ArrowDownOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

function onChange(e) {
  console.log(`checked = ${e.target.checked}`);
}

const options = [{ value: 'red' }];

const BulkImportTab = () => {
  const { t } = useTranslation();

  function tagRender(props) {
    const { label, value, closable, onClose } = props;
    const onPreventMouseDown = event => {
      event.preventDefault();
      event.stopPropagation();
    };
    return (
      <Tag
        color={value}
        onMouseDown={onPreventMouseDown}
        closable={closable}
        onClose={onClose}
        style={{ marginRight: 3 }}
      >
        {label}
      </Tag>
    );
  }

  return (
    <div className="BulkImportTab">
      <div className="Col">
        <div className="TitleContainer">
          <h3>Assign to a Group</h3>
          <p>Assign contacts to a single or multiple Group</p>
        </div>
        <div className="Col">
          <Select
            mode="multiple"
            showArrow
            tagRender={tagRender}
            defaultValue={['red']}
            style={{ width: '250px' }}
            options={options}
          />
          <Checkbox onChange={onChange}>
            Skip this step if you do not want the contacts to be included in any group.
          </Checkbox>
        </div>
      </div>
      <div className="Col">
        <div className="TitleContainer">
          <h3>Complete Spreadsheet</h3>
          <p>
            Download the spreadsheet, complete with the required information and upload the
            document.
          </p>
        </div>
        <div className="row">
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              icon: <ArrowDownOutlined />
            }}
            buttonText={'Download Spreadsheet'}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-outline, disable',
              icon: <ArrowDownOutlined />
            }}
            buttonText={'Download Spreadsheet'}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-outline, disable',
              icon: <ArrowDownOutlined />
            }}
            buttonText={'Download Spreadsheet'}
          />
        </div>
      </div>
    </div>
  );
};

export default BulkImportTab;
