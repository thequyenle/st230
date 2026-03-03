package com.dress.game.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dress.game.data.local.dao.UserDao
import com.dress.game.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}