/**
 * 打卡（Check-in）API 模块
 * --------------------------------------------------
 * 围绕「每日心情打卡」功能：新增打卡 / 查询日历 / 查看情侣双方某日记录。
 */
import request from '../utils/request'

/**
 * 新增一条打卡记录（同一天同一用户仅可一条）
 * @param {{checkinDate: string, moodEmoji: string, moodText: string, content: string}} data
 * @returns {Promise<any>}
 */
export const addCheckin = data => request.post('/api/checkins', data)

/**
 * 获取指定月份的打卡日历数据
 * @param {string} month 形如 "2024-05"
 * @returns {Promise<Array>} 当月所有打卡条目（包含双方）
 */
export const getCheckinCalendar = month => request.get('/api/checkins/calendar', { params: { month } })

/**
 * 获取情侣双方在某一天的全部打卡详情
 * @param {string} date 日期，格式 YYYY-MM-DD
 * @returns {Promise<Array>}
 */
export const getCoupleCheckins = date => request.get('/api/checkins/couple', { params: { date } })
